package uk.gov.justice.digital.hmpps.arnscoordinatorapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.dto.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.dto.SubjectDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.CoordinatorException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.IdentifierType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.PropertyValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.Collection
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.CollectionItem
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.SingleValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.Value
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.plan.PlanState
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.plan.PlanType
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class SentencePlanServiceTest {
  private val service = SentencePlanService()

  private val request = OasysCreateRequest(
    oasysAssessmentPk = "1001",
    planType = PlanType.REVIEW,
    assessmentType = AssessmentType.SAN_SP,
    userDetails = OasysUserDetails(id = "user-id", name = "User Name"),
  )

  @Nested
  inner class Create {
    @Test
    fun `builds a SENTENCE_PLAN create command`() {
      val command = service.create(request)

      assertThat(command.assessmentType).isEqualTo("SENTENCE_PLAN")
      assertThat(command.formVersion).isEqualTo("v1.0")
      assertThat(command.properties).isEqualTo(mapOf("PLAN_TYPE" to PropertyValue(type = "Single", value = "REVIEW")))
      assertThat(command.flags).containsExactly("SAN_BETA")
      assertThat(command.user).isEqualTo(AAPUser(id = "user-id", name = "User Name"))
      assertThat(command.identifiers).isNull()
      assertThat(command.hooks).isNull()
    }

    @Test
    fun `has no flags for an SP assessment`() {
      assertThat(service.create(request.copy(assessmentType = AssessmentType.SP)).flags).isEmpty()
    }

    @Test
    fun `includes the subject identifiers`() {
      val command = service.create(request.copy(subjectDetails = SubjectDetails(crn = "X123456", nomisId = "A1234BC")))

      assertThat(command.identifiers).isEqualTo(mapOf(IdentifierType.CRN to "X123456", IdentifierType.NOMIS_ID to "A1234BC"))
    }
  }

  @Nested
  inner class Fetch {
    private val entityUuid = UUID.randomUUID()
    private val updatedAt = LocalDateTime.parse("2026-01-09T12:30:00")

    private fun agreement(status: String?, updatedAt: LocalDateTime) = CollectionItem(
      uuid = UUID.randomUUID(),
      createdAt = updatedAt,
      updatedAt = updatedAt,
      answers = emptyMap(),
      properties = status?.let { mapOf("status" to SingleValue(it)) } ?: emptyMap(),
      collections = emptyList(),
    )

    private fun result(
      properties: Map<String, Value> = mapOf("PLAN_TYPE" to SingleValue("INITIAL")),
      agreements: List<CollectionItem>? = null,
    ) = AssessmentVersionQueryResult(
      assessmentUuid = entityUuid,
      aggregateUuid = UUID.randomUUID(),
      assessmentType = "SENTENCE_PLAN",
      formVersion = "v1.0",
      createdAt = updatedAt.minusDays(1),
      updatedAt = updatedAt,
      answers = emptyMap(),
      properties = properties,
      collections = listOfNotNull(
        agreements?.let { Collection(uuid = UUID.randomUUID(), createdAt = updatedAt, updatedAt = updatedAt, name = "PLAN_AGREEMENTS", items = it) },
      ),
      collaborators = emptySet(),
      identifiers = emptyMap(),
    )

    private fun fetch(result: AssessmentVersionQueryResult) = OasysGetResponse().also { service.fetch(result, it) }

    @Test
    fun `maps the plan into the response`() {
      val response = fetch(result())

      assertThat(response.sentencePlanId).isEqualTo(entityUuid)
      assertThat(response.sentencePlanVersion).isEqualTo(updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli())
      assertThat(response.lastUpdatedTimestampSP).isEqualTo(updatedAt)
      assertThat(response.planType).isEqualTo(PlanType.INITIAL)
    }

    @ParameterizedTest
    @EnumSource(PlanType::class)
    fun `maps every plan type`(planType: PlanType) {
      assertThat(fetch(result(properties = mapOf("PLAN_TYPE" to SingleValue(planType.name)))).planType).isEqualTo(planType)
    }

    @Test
    fun `throws when there is no PLAN_TYPE`() {
      val exception = assertThrows<CoordinatorException> { fetch(result(properties = emptyMap())) }

      assertThat(exception.message).isEqualTo("No value for PLAN_TYPE for entity $entityUuid")
    }

    @Test
    fun `throws when the PLAN_TYPE is not recognised`() {
      assertThrows<IllegalArgumentException> { fetch(result(properties = mapOf("PLAN_TYPE" to SingleValue("NOT_A_PLAN_TYPE")))) }
    }

    @Test
    fun `is INCOMPLETE when there is no PLAN_AGREEMENTS collection`() {
      assertThat(fetch(result()).planComplete).isEqualTo(PlanState.INCOMPLETE)
    }

    @Test
    fun `is INCOMPLETE when there are no agreements`() {
      assertThat(fetch(result(agreements = emptyList())).planComplete).isEqualTo(PlanState.INCOMPLETE)
    }

    @ParameterizedTest
    @ValueSource(strings = ["AGREED", "COULD_NOT_ANSWER", "DO_NOT_AGREE"])
    fun `is COMPLETE when the latest agreement is not a draft`(status: String) {
      assertThat(fetch(result(agreements = listOf(agreement(status, updatedAt)))).planComplete).isEqualTo(PlanState.COMPLETE)
    }

    @Test
    fun `is INCOMPLETE when the latest agreement is a draft`() {
      assertThat(fetch(result(agreements = listOf(agreement("DRAFT", updatedAt)))).planComplete).isEqualTo(PlanState.INCOMPLETE)
    }

    @Test
    fun `is INCOMPLETE when the latest agreement has no status`() {
      assertThat(fetch(result(agreements = listOf(agreement(null, updatedAt)))).planComplete).isEqualTo(PlanState.INCOMPLETE)
    }

    @Test
    fun `uses the most recently updated agreement`() {
      val olderAgreed = agreement("AGREED", updatedAt.minusDays(1))
      val newerDraft = agreement("DRAFT", updatedAt)

      assertThat(fetch(result(agreements = listOf(newerDraft, olderAgreed))).planComplete).isEqualTo(PlanState.INCOMPLETE)
      assertThat(fetch(result(agreements = listOf(agreement("DRAFT", updatedAt.minusDays(1)), agreement("AGREED", updatedAt)))).planComplete)
        .isEqualTo(PlanState.COMPLETE)
    }
  }

  @Test
  fun `reset builds a properties command for the plan`() {
    val entityUuid = UUID.randomUUID()

    val command = service.reset(request, entityUuid)

    assertThat(command.assessmentUuid).isEqualTo(entityUuid)
    assertThat(command.user).isEqualTo(AAPUser(id = "user-id", name = "User Name"))
  }
}

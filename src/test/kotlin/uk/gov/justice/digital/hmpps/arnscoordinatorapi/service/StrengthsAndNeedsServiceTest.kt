package uk.gov.justice.digital.hmpps.arnscoordinatorapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import tools.jackson.core.JacksonException
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.dto.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.dto.SubjectDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.IdentifierType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook.UpdateOasysDataMapping
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.MultiValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.SingleValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.Value
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.plan.PlanType
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.stream.Stream

class StrengthsAndNeedsServiceTest {
  private val service = StrengthsAndNeedsService(jacksonObjectMapper())

  @Nested
  inner class Create {
    private val request = OasysCreateRequest(
      oasysAssessmentPk = "1001",
      planType = PlanType.INITIAL,
      assessmentType = AssessmentType.SAN_SP,
      userDetails = OasysUserDetails(id = "user-id", name = "User Name"),
      subjectDetails = SubjectDetails(crn = "X123456"),
    )

    @Test
    fun `builds a STRENGTHS_AND_NEEDS create command`() {
      val command = service.create(request)

      assertThat(command.assessmentType).isEqualTo("STRENGTHS_AND_NEEDS")
      assertThat(command.formVersion).isEqualTo("v1.0")
      assertThat(command.flags).containsExactly("SAN_BETA")
      assertThat(command.user).isEqualTo(AAPUser(id = "user-id", name = "User Name"))
      assertThat(command.identifiers).isEqualTo(mapOf(IdentifierType.CRN to "X123456"))
      assertThat(command.properties).isNull()
    }

    @Test
    fun `attaches the OASys data mapping hook built from the v1 form config`() {
      val hook = service.create(request).hooks?.single() as UpdateOasysDataMapping

      assertThat(hook.formConfig.fields).isNotEmpty()
    }
  }

  @Nested
  inner class Fetch {
    private val entityUuid = UUID.randomUUID()
    private val updatedAt = LocalDateTime.parse("2026-01-09T12:30:00")

    private fun result(
      answers: Map<String, Value> = emptyMap(),
      properties: Map<String, Value> = emptyMap(),
    ) = AssessmentVersionQueryResult(
      assessmentUuid = entityUuid,
      aggregateUuid = UUID.randomUUID(),
      assessmentType = "STRENGTHS_AND_NEEDS",
      formVersion = "v1.0",
      createdAt = updatedAt.minusDays(1),
      updatedAt = updatedAt,
      answers = answers,
      properties = properties,
      collections = emptyList(),
      collaborators = emptySet(),
      identifiers = emptyMap(),
    )

    private fun fetch(result: AssessmentVersionQueryResult) = OasysGetResponse().also { service.fetch(result, it) }

    @Test
    fun `maps the assessment into the response`() {
      val response = fetch(result(answers = mapOf("q1" to SingleValue("a1"), "q2" to MultiValue(listOf("a2", "a3")))))

      assertThat(response.sanAssessmentId).isEqualTo(entityUuid)
      assertThat(response.sanAssessmentVersion).isEqualTo(updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli())
      assertThat(response.lastUpdatedTimestampSAN).isEqualTo(updatedAt)
      assertThat(response.sanAssessmentData).isEqualTo(
        mapOf(
          "q1" to mapOf("value" to "a1"),
          "q2" to mapOf("values" to listOf("a2", "a3")),
        ),
      )
    }

    @Test
    fun `includes only section complete properties in the assessment data`() {
      val response = fetch(
        result(
          answers = mapOf("alcohol_use_changes" to SingleValue("MADE_CHANGES")),
          properties = mapOf(
            "alcohol_use_section_complete" to SingleValue("YES"),
            "oasys_equivalent" to SingleValue("{}"),
            "extra_stuff_in_properties" to SingleValue("SHOULD_NOT_BE_INCLUDED"),
          ),
        ),
      )

      assertThat(response.sanAssessmentData).isEqualTo(
        mapOf(
          "alcohol_use_changes" to mapOf("value" to "MADE_CHANGES"),
          "alcohol_use_section_complete" to mapOf("value" to "YES"),
        ),
      )
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.arnscoordinatorapi.service.StrengthsAndNeedsServiceTest#escapedCharacters")
    fun `unescapes HTML entities in the answers and OASys equivalent`(escaped: String, unescaped: String) {
      val response = fetch(
        result(
          answers = mapOf("q1" to SingleValue("before${escaped}after")),
          properties = mapOf("oasys_equivalent" to SingleValue("""{"o1":"before${escaped}after"}""")),
        ),
      )

      assertThat(response.sanAssessmentData).isEqualTo(mapOf("q1" to mapOf("value" to "before${unescaped}after")))
      assertThat(response.sanOasysEquivalent).isEqualTo(mapOf("o1" to "before${unescaped}after"))
    }

    @Test
    fun `unescapes a realistic free text answer`() {
      val response = fetch(result(answers = mapOf("q1" to SingleValue("¯&#x5C;_(ツ)_&#x2F;¯"))))

      assertThat(response.sanAssessmentData).isEqualTo(mapOf("q1" to mapOf("value" to "¯\\_(ツ)_/¯")))
    }

    @Test
    fun `OASys equivalent is empty when the property is absent`() {
      assertThat(fetch(result()).sanOasysEquivalent).isEmpty()
    }

    @Test
    fun `OASys equivalent is parsed from the stringified JSON property`() {
      val response = fetch(
        result(properties = mapOf("oasys_equivalent" to SingleValue("""{"1.2":"YES","2.1":["A","B"],"nested":{"key":"value"}}"""))),
      )

      assertThat(response.sanOasysEquivalent).isEqualTo(
        mapOf(
          "1.2" to "YES",
          "2.1" to listOf("A", "B"),
          "nested" to mapOf("key" to "value"),
        ),
      )
    }

    @Test
    fun `throws when the OASys equivalent is not valid JSON`() {
      assertThrows<JacksonException> { fetch(result(properties = mapOf("oasys_equivalent" to SingleValue("not-json")))) }
    }
  }

  companion object {
    @JvmStatic
    fun escapedCharacters(): Stream<Arguments> = Stream.of(
      Arguments.of("&amp;", "&"),
      Arguments.of("&#x27;", "'"),
      Arguments.of("&lt;", "<"),
      Arguments.of("&gt;", ">"),
      Arguments.of("&#x2F;", "/"),
      Arguments.of("&#x5C;", "\\"),
      Arguments.of("&#96;", "`"),
      Arguments.of("&quot;", "\""),
    )
  }
}

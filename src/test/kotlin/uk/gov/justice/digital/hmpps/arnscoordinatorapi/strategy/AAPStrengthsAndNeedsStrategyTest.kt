package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.AAPApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.MultiValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.SingleValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.Value
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.AssessmentResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.service.OasysVersionService
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class AAPStrengthsAndNeedsStrategyTest {
  private val aapApi: AAPApi = mock()
  private val oasysVersionService: OasysVersionService = mock()
  private val clock: Clock = mock()
  private val objectMapper = jacksonObjectMapper()
  private lateinit var strategy: AAPStrengthsAndNeedsStrategy

  private val now = LocalDateTime.parse("2026-01-09T12:00:00")

  @BeforeEach
  fun setup() {
    strategy = AAPStrengthsAndNeedsStrategy(
      aapApi = aapApi,
      oasysVersionService = oasysVersionService,
      clock = clock,
      objectMapper = objectMapper,
    )

    whenever(clock.now()).thenReturn(now)
  }

  @Nested
  inner class Fetch {

    private fun queryResult(
      entityUuid: UUID,
      properties: Map<String, Value> = emptyMap(),
      answers: Map<String, Value> = emptyMap(),
    ) = AssessmentVersionQueryResult(
      assessmentUuid = entityUuid,
      aggregateUuid = UUID.randomUUID(),
      assessmentType = "STRENGTHS_AND_NEEDS",
      formVersion = "v1.0",
      createdAt = LocalDateTime.parse("2026-01-09T12:00:00"),
      updatedAt = LocalDateTime.parse("2026-01-09T12:30:00"),
      answers = answers,
      properties = properties,
      collections = emptyList(),
      collaborators = emptySet(),
      identifiers = emptyMap(),
    )

    @Test
    fun `maps the assessment response with metadata and answers`() {
      val entityUuid = UUID.randomUUID()
      val result = queryResult(
        entityUuid,
        answers = mapOf("q1" to SingleValue("a1"), "q2" to MultiValue(listOf("a2", "a3"))),
      )

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(AAPApi.ApiOperationResult.Success(result))

      val operationResult = strategy.fetch(entityUuid)

      assertThat(operationResult).isInstanceOf(OperationResult.Success::class.java)
      val response = (operationResult as OperationResult.Success<*>).data as AssessmentResponse
      assertThat(response.metaData.uuid).isEqualTo(entityUuid)
      assertThat(response.metaData.createdAt).isEqualTo(result.createdAt)
      assertThat(response.metaData.versionCreatedAt).isEqualTo(result.updatedAt)
      assertThat(response.metaData.versionUpdatedAt).isEqualTo(result.updatedAt)
      assertThat(response.metaData.versionNumber)
        .isEqualTo(result.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli())
      assertThat(response.metaData.formVersion).isEqualTo("v1.0")
      assertThat(response.assessment).isEqualTo(result.answers)
      verify(aapApi).fetchAssessment(entityUuid, now)
    }

    @Test
    fun `oasysEquivalent is empty when the property is absent`() {
      val entityUuid = UUID.randomUUID()

      whenever(aapApi.fetchAssessment(entityUuid, now))
        .thenReturn(AAPApi.ApiOperationResult.Success(queryResult(entityUuid)))

      val operationResult = strategy.fetch(entityUuid)

      val response = (operationResult as OperationResult.Success<*>).data as AssessmentResponse
      assertThat(response.oasysEquivalent).isEmpty()
    }

    @Test
    fun `oasysEquivalent is parsed from the stringified JSON property`() {
      val entityUuid = UUID.randomUUID()
      val oasysEquivalentJson = """{"1.2":"YES","2.1":["A","B"],"nested":{"key":"value"}}"""

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Success(
          queryResult(entityUuid, properties = mapOf("oasys_equivalent" to SingleValue(oasysEquivalentJson))),
        ),
      )

      val operationResult = strategy.fetch(entityUuid)

      val response = (operationResult as OperationResult.Success<*>).data as AssessmentResponse
      assertThat(response.oasysEquivalent).isEqualTo(
        mapOf(
          "1.2" to "YES",
          "2.1" to listOf("A", "B"),
          "nested" to mapOf("key" to "value"),
        ),
      )
    }

    @Test
    fun `oasysEquivalent is empty when the property value is an empty JSON object`() {
      val entityUuid = UUID.randomUUID()

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Success(
          queryResult(entityUuid, properties = mapOf("oasys_equivalent" to SingleValue("{}"))),
        ),
      )

      val operationResult = strategy.fetch(entityUuid)

      val response = (operationResult as OperationResult.Success<*>).data as AssessmentResponse
      assertThat(response.oasysEquivalent).isEmpty()
    }

    @Test
    fun `returns failure when the oasys_equivalent property is not valid JSON`() {
      val entityUuid = UUID.randomUUID()

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Success(
          queryResult(entityUuid, properties = mapOf("oasys_equivalent" to SingleValue("not-json"))),
        ),
      )

      val operationResult = strategy.fetch(entityUuid)

      assertThat(operationResult).isInstanceOf(OperationResult.Failure::class.java)
      assertThat((operationResult as OperationResult.Failure).errorMessage)
        .isEqualTo("Failed to fetch Strengths and Needs assessment for entity $entityUuid")
    }

    @Test
    fun `returns failure when the fetch call fails`() {
      val entityUuid = UUID.randomUUID()

      whenever(aapApi.fetchAssessment(entityUuid, now))
        .thenReturn(AAPApi.ApiOperationResult.Failure("Fetch error occurred"))

      val operationResult = strategy.fetch(entityUuid)

      assertThat(operationResult).isInstanceOf(OperationResult.Failure::class.java)
      assertThat((operationResult as OperationResult.Failure).errorMessage).isEqualTo("Fetch error occurred")
    }
  }
}

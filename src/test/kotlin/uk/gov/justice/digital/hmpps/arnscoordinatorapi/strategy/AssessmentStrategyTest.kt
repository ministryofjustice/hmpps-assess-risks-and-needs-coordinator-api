package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.StrengthsAndNeedsApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CounterSignAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.RollbackData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.AssessmentMetadata
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.AssessmentResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.time.LocalDateTime
import java.util.UUID

class AssessmentStrategyTest {
  private val strengthsAndNeedsApi: StrengthsAndNeedsApi = mock()
  private lateinit var assessmentStrategy: AssessmentStrategy

  @BeforeEach
  fun setup() {
    assessmentStrategy = AssessmentStrategy(strengthsAndNeedsApi)
  }

  @Nested
  inner class Create {

    @Test
    fun `should return success when create assessment is successful`() {
      val createData = CreateData(
        assessment = CreateAssessmentData(UserDetails("id", "name")),
      )
      val versionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.ASSESSMENT)

      `when`(strengthsAndNeedsApi.createAssessment(any())).thenReturn(
        StrengthsAndNeedsApi.ApiOperationResult.Success(versionedEntity),
      )

      val result = assessmentStrategy.create(createData)

      assertTrue(result is OperationResult.Success)
      assertEquals(versionedEntity, (result as OperationResult.Success).data)
      verify(strengthsAndNeedsApi).createAssessment(any())
    }

    @Test
    fun `should return failure when create assessment fails`() {
      val createData = CreateData(
        assessment = CreateAssessmentData(UserDetails("id", "name")),
      )

      `when`(strengthsAndNeedsApi.createAssessment(any())).thenReturn(
        StrengthsAndNeedsApi.ApiOperationResult.Failure("Error occurred"),
      )

      val result = assessmentStrategy.create(createData)

      assertTrue(result is OperationResult.Failure)
      assertEquals("Error occurred", (result as OperationResult.Failure).errorMessage)
      verify(strengthsAndNeedsApi).createAssessment(any())
    }
  }

  @Nested
  inner class Fetch {

    @Test
    fun `should return success when fetch assessment is successful`() {
      val entityUuid = UUID.randomUUID()
      val getAssessmentResponse = AssessmentResponse(
        metaData = AssessmentMetadata(
          uuid = entityUuid,
          createdAt = LocalDateTime.now(),
          versionUuid = UUID.randomUUID(),
          versionNumber = 1,
          versionCreatedAt = LocalDateTime.now(),
          versionUpdatedAt = LocalDateTime.now(),
          formVersion = "1.0",
        ),
        assessment = emptyMap<String, Any>(),
        oasysEquivalent = emptyMap<String, Any>(),
      )

      `when`(strengthsAndNeedsApi.getAssessment(entityUuid)).thenReturn(
        StrengthsAndNeedsApi.ApiOperationResult.Success(getAssessmentResponse),
      )

      val result = assessmentStrategy.fetch(entityUuid)

      assertTrue(result is OperationResult.Success)
      assertEquals(getAssessmentResponse, (result as OperationResult.Success).data)
      verify(strengthsAndNeedsApi).getAssessment(entityUuid)
    }

    @Test
    fun `should return failure when fetch assessment fails`() {
      val entityUuid = UUID.randomUUID()

      `when`(strengthsAndNeedsApi.getAssessment(entityUuid)).thenReturn(
        StrengthsAndNeedsApi.ApiOperationResult.Failure("Fetch error occurred"),
      )

      val result = assessmentStrategy.fetch(entityUuid)

      assertTrue(result is OperationResult.Failure)
      assertEquals("Fetch error occurred", (result as OperationResult.Failure).errorMessage)
      verify(strengthsAndNeedsApi).getAssessment(entityUuid)
    }
  }

  @Nested
  inner class Rollback {
    val request = OasysRollbackRequest(
      sanVersionNumber = 1,
      sentencePlanVersionNumber = null,
      userDetails = OasysUserDetails("id", "name"),
    )
    val rollbackData = RollbackData.from(request)
    val versionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.ASSESSMENT)

    @Test
    fun `should return success when rollback assessment is successful`() {
      `when`(strengthsAndNeedsApi.rollback(rollbackData, versionedEntity.id)).thenReturn(
        StrengthsAndNeedsApi.ApiOperationResultExtended.Success(versionedEntity),
      )

      val result = assessmentStrategy.rollback(request, versionedEntity.id)

      assertTrue(result is OperationResult.Success)
      assertEquals(versionedEntity, (result as OperationResult.Success).data)
      verify(strengthsAndNeedsApi).rollback(rollbackData, versionedEntity.id)
    }

    @Test
    fun `should return failure when create assessment fails`() {
      `when`(strengthsAndNeedsApi.rollback(rollbackData, versionedEntity.id)).thenReturn(
        StrengthsAndNeedsApi.ApiOperationResultExtended.Failure("Error occurred"),
      )

      val result = assessmentStrategy.rollback(request, versionedEntity.id)

      assertTrue(result is OperationResult.Failure)
      assertEquals("Error occurred", (result as OperationResult.Failure).errorMessage)
      verify(strengthsAndNeedsApi).rollback(rollbackData, versionedEntity.id)
    }
  }

  @Nested
  inner class CounterSign {
    private val entityUuid = UUID.randomUUID()
    private val request = OasysCounterSignRequest(
      sanVersionNumber = 1,
      sentencePlanVersionNumber = 1,
      outcome = CounterSignOutcome.COUNTERSIGNED,
      userDetails = OasysUserDetails("1", "OASys User"),
    )

    @Test
    fun `should return success when countersign assessment is successful`() {
      val versionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.ASSESSMENT)

      `when`(strengthsAndNeedsApi.counterSign(entityUuid, CounterSignAssessmentData.from(request))).thenReturn(
        StrengthsAndNeedsApi.ApiOperationResultExtended.Success(versionedEntity),
      )

      val result = assessmentStrategy.counterSign(
        entityUuid,
        OasysCounterSignRequest(
          sanVersionNumber = 1,
          sentencePlanVersionNumber = 1,
          outcome = CounterSignOutcome.COUNTERSIGNED,
          userDetails = OasysUserDetails("1", "OASys User"),
        ),
      )

      verify(strengthsAndNeedsApi).counterSign(entityUuid, CounterSignAssessmentData.from(request))
      assertTrue(result is OperationResult.Success)
      assertEquals(versionedEntity, (result as OperationResult.Success).data)
    }

    @Test
    fun `should return failure when countersign assessment fails`() {
      `when`(strengthsAndNeedsApi.counterSign(entityUuid, CounterSignAssessmentData.from(request))).thenReturn(
        StrengthsAndNeedsApi.ApiOperationResultExtended.Failure("Failed to countersign"),
      )

      val result = assessmentStrategy.counterSign(
        entityUuid,
        OasysCounterSignRequest(
          sanVersionNumber = 1,
          sentencePlanVersionNumber = 1,
          outcome = CounterSignOutcome.COUNTERSIGNED,
          userDetails = OasysUserDetails("1", "OASys User"),
        ),
      )

      verify(strengthsAndNeedsApi).counterSign(entityUuid, CounterSignAssessmentData.from(request))
      assertTrue(result is OperationResult.Failure)
      assertEquals("Failed to countersign", (result as OperationResult.Failure).errorMessage)
    }
  }
}

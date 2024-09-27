package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.StrengthsAndNeedsApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

class AssessmentServiceTest {
  private val strengthsAndNeedsApi = mock(StrengthsAndNeedsApi::class.java)
  private val assessmentService = AssessmentService(strengthsAndNeedsApi)
  private val userDetails = UserDetails(id = "user123", name = "John Doe")

  @Nested
  inner class Create {

    @Test
    fun `should return success when StrengthsAndNeedsApi returns success`() {
      val createAssessmentData = CreateAssessmentData(userDetails = userDetails)
      val createData = CreateData(
        assessment = createAssessmentData,
      )
      val versionedEntity = VersionedEntity(
        id = UUID.randomUUID(),
        version = 1L,
        entityType = EntityType.ASSESSMENT,
      )
      val successResult = StrengthsAndNeedsApi.ApiOperationResult.Success(versionedEntity)

      `when`(strengthsAndNeedsApi.createAssessment(createAssessmentData)).thenReturn(successResult)

      val result = assessmentService.create(createData)

      assertTrue(result is OperationResult.Success)
      assertEquals(versionedEntity, (result as OperationResult.Success).data)
      verify(strengthsAndNeedsApi).createAssessment(createAssessmentData)
    }

    @Test
    fun `should return failure when StrengthsAndNeedsApi returns failure`() {
      val createAssessmentData = CreateAssessmentData(userDetails = userDetails)
      val createData = CreateData(
        assessment = createAssessmentData,
      )
      val failureResult = StrengthsAndNeedsApi.ApiOperationResult.Failure<Nothing>("An error occurred")

      `when`(strengthsAndNeedsApi.createAssessment(createAssessmentData)).thenReturn(failureResult)

      val result = assessmentService.create(createData)

      assertTrue(result is OperationResult.Failure)
      assertEquals("An error occurred", (result as OperationResult.Failure).errorMessage)
      verify(strengthsAndNeedsApi).createAssessment(createAssessmentData) // Correctly verifying the mock
    }
  }
}

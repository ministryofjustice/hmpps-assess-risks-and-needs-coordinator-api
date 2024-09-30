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
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.StrengthsAndNeedsApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
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
}
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class OasysAssociationsServiceTest {

  @Mock
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @InjectMocks
  lateinit var oasysAssociationsService: OasysAssociationsService

  @Nested
  inner class EnsureNoExistingAssociationTest {

    @Test
    fun `should return success when no existing associations`() {
      val oasysAssessmentPk = "test-pk"
      `when`(oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk))
        .thenReturn(emptyList())

      val result = oasysAssociationsService.ensureNoExistingAssociation(oasysAssessmentPk)

      assertTrue(result is OperationResult.Success)
      verify(oasysAssociationRepository).findAllByOasysAssessmentPk(oasysAssessmentPk)
    }

    @Test
    fun `should return failure when associations exist`() {
      val oasysAssessmentPk = "test-pk"
      val association = OasysAssociation(
        id = 1L,
        entityType = EntityType.ASSESSMENT,
        oasysAssessmentPk = oasysAssessmentPk,
        entityUuid = UUID.randomUUID(),
      )
      `when`(oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk))
        .thenReturn(listOf(association))

      val result = oasysAssociationsService.ensureNoExistingAssociation(oasysAssessmentPk)

      assertTrue(result is OperationResult.Failure)
      assertTrue((result as OperationResult.Failure).errorMessage.contains("ASSESSMENT"))
      verify(oasysAssociationRepository).findAllByOasysAssessmentPk(oasysAssessmentPk)
    }
  }

  @Nested
  inner class StoreAssociationTest {

    @Test
    fun `should return success when storing association`() {
      val association = OasysAssociation(
        id = 1L,
        entityType = EntityType.ASSESSMENT,
        oasysAssessmentPk = "test-pk",
        entityUuid = UUID.randomUUID(),
      )
      `when`(oasysAssociationRepository.save(association)).thenReturn(association)

      val result = oasysAssociationsService.storeAssociation(association)

      assertTrue(result is OperationResult.Success)
      verify(oasysAssociationRepository).save(association)
    }

    @Test
    fun `should return failure when storing association throws exception`() {
      val association = OasysAssociation(
        id = 1L,
        entityType = EntityType.ASSESSMENT,
        oasysAssessmentPk = "test-pk",
        entityUuid = UUID.randomUUID(),
      )
      `when`(oasysAssociationRepository.save(association)).thenThrow(RuntimeException("Database error"))

      val result = oasysAssociationsService.storeAssociation(association)

      assertTrue(result is OperationResult.Failure)
      assertTrue((result as OperationResult.Failure).errorMessage.contains("Failed to store association"))
      verify(oasysAssociationRepository).save(association)
    }
  }
}

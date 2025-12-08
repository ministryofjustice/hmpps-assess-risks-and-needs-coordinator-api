package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations

import org.junit.jupiter.api.Assertions.assertEquals
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
  inner class StoreAssociation {

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

  @Nested
  inner class FindAssociations {

    @Test
    fun `should return empty list when no associations are found`() {
      val oasysAssessmentPk = "test-pk"
      `when`(oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk))
        .thenReturn(emptyList())

      val result = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

      assertTrue(result.isEmpty())
      verify(oasysAssociationRepository).findAllByOasysAssessmentPk(oasysAssessmentPk)
    }

    @Test
    fun `should return list of associations when associations are found`() {
      val oasysAssessmentPk = "test-pk"
      val association = OasysAssociation(
        id = 1L,
        entityType = EntityType.ASSESSMENT,
        oasysAssessmentPk = oasysAssessmentPk,
        entityUuid = UUID.randomUUID(),
      )
      `when`(oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk))
        .thenReturn(listOf(association))

      val result = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

      assertEquals(1, result.size)
      assertEquals(association, result[0])
      verify(oasysAssociationRepository).findAllByOasysAssessmentPk(oasysAssessmentPk)
    }
  }

  @Nested
  inner class FindAssociationsWithType {

    @Test
    fun `should return empty list when no associations are found with matching type`() {
      val oasysAssessmentPk = "test-pk"
      `when`(oasysAssociationRepository.findAllByOasysAssessmentPkAndEntityTypeIn(oasysAssessmentPk, listOf(EntityType.PLAN)))
        .thenReturn(emptyList())

      val result = oasysAssociationsService.findAssociationsByPkAndType(oasysAssessmentPk, listOf(EntityType.PLAN))

      assertTrue(result.isEmpty())
      verify(oasysAssociationRepository).findAllByOasysAssessmentPkAndEntityTypeIn(oasysAssessmentPk, listOf(EntityType.PLAN))
    }

    @Test
    fun `should return list of associations when associations are found with type`() {
      val oasysAssessmentPk = "test-pk"
      val association = OasysAssociation(
        id = 1L,
        entityType = EntityType.ASSESSMENT,
        oasysAssessmentPk = oasysAssessmentPk,
        entityUuid = UUID.randomUUID(),
      )
      `when`(oasysAssociationRepository.findAllByOasysAssessmentPkAndEntityTypeIn(oasysAssessmentPk, listOf(EntityType.ASSESSMENT)))
        .thenReturn(listOf(association))

      val result = oasysAssociationsService.findAssociationsByPkAndType(oasysAssessmentPk, listOf(EntityType.ASSESSMENT))

      assertEquals(1, result.size)
      assertEquals(association, result[0])
      verify(oasysAssociationRepository).findAllByOasysAssessmentPkAndEntityTypeIn(oasysAssessmentPk, listOf(EntityType.ASSESSMENT))
    }
  }

  @Nested
  inner class FindAllIncludingDeleted {
    val entityUuid = UUID.randomUUID()

    @Test
    fun `should return empty list when no associations are found`() {
      `when`(oasysAssociationRepository.findAllByEntityUuidIncludingDeleted(entityUuid))
        .thenReturn(emptyList())

      assertTrue(oasysAssociationsService.findAllIncludingDeleted(entityUuid).isEmpty())

      verify(oasysAssociationRepository).findAllByEntityUuidIncludingDeleted(entityUuid)
    }

    @Test
    fun `should return list of associations when associations are found`() {
      val associations = listOf(OasysAssociation())

      `when`(oasysAssociationRepository.findAllByEntityUuidIncludingDeleted(entityUuid)).thenReturn(associations)

      assertEquals(associations, oasysAssociationsService.findAllIncludingDeleted(entityUuid))

      verify(oasysAssociationRepository).findAllByEntityUuidIncludingDeleted(entityUuid)
    }
  }

  @Nested
  inner class FindDeletedAssociations {
    val oasysAssessmentPk = "test"

    @Test
    fun `should return empty list when no associations are found`() {
      `when`(oasysAssociationRepository.findAllDeletedByOasysAssessmentPk(oasysAssessmentPk))
        .thenReturn(emptyList())

      assertTrue(oasysAssociationsService.findDeletedAssociations(oasysAssessmentPk).isEmpty())

      verify(oasysAssociationRepository).findAllDeletedByOasysAssessmentPk(oasysAssessmentPk)
    }

    @Test
    fun `should return list of associations when associations are found`() {
      val associations = listOf(OasysAssociation())

      `when`(oasysAssociationRepository.findAllDeletedByOasysAssessmentPk(oasysAssessmentPk)).thenReturn(associations)

      assertEquals(associations, oasysAssociationsService.findDeletedAssociations(oasysAssessmentPk))

      verify(oasysAssociationRepository).findAllDeletedByOasysAssessmentPk(oasysAssessmentPk)
    }
  }
}

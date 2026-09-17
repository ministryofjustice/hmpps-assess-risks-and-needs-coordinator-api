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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.EntityAssociationDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentTypeConfig
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class OasysAssociationsServiceTest {

  @Mock
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @Mock
  lateinit var assessmentTypeConfig: AssessmentTypeConfig

  @InjectMocks
  lateinit var oasysAssociationsService: OasysAssociationsService

  @Nested
  inner class EnsureNoExistingAssociation {

    @Test
    fun `should return success when no existing associations`() {
      val oasysAssessmentPk = "test-pk"
      `when`(oasysAssociationRepository.findAllByOasysAssessmentPkAndEntityTypeIn(eq(oasysAssessmentPk), any()))
        .thenReturn(emptyList())

      val result = oasysAssociationsService.ensureNoExistingAssociation(oasysAssessmentPk)

      assertTrue(result is OperationResult.Success)
      verify(oasysAssociationRepository).findAllByOasysAssessmentPkAndEntityTypeIn(eq(oasysAssessmentPk), any())
    }

    @Test
    fun `should return failure when associations exist`() {
      val oasysAssessmentPk = "test-pk"
      val association = OasysAssociation(
        id = 1L,
        entityType = EntityType.AAP_SAN,
        oasysAssessmentPk = oasysAssessmentPk,
        entityUuid = UUID.randomUUID(),
      )
      `when`(oasysAssociationRepository.findAllByOasysAssessmentPkAndEntityTypeIn(eq(oasysAssessmentPk), any()))
        .thenReturn(listOf(association))

      val result = oasysAssociationsService.ensureNoExistingAssociation(oasysAssessmentPk)

      assertTrue(result is OperationResult.Failure)
      assertTrue((result as OperationResult.Failure).errorMessage.contains("AAP_SAN"))
      verify(oasysAssociationRepository).findAllByOasysAssessmentPkAndEntityTypeIn(eq(oasysAssessmentPk), any())
    }
  }

  @Nested
  inner class StoreAssociation {

    @Test
    fun `should return success when storing association`() {
      val association = OasysAssociation(
        id = 1L,
        entityType = EntityType.AAP_SAN,
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
        entityType = EntityType.AAP_SAN,
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
      `when`(oasysAssociationRepository.findAllByOasysAssessmentPkAndEntityTypeIn(eq(oasysAssessmentPk), any()))
        .thenReturn(emptyList())

      val result = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

      assertTrue(result.isEmpty())
      verify(oasysAssociationRepository).findAllByOasysAssessmentPkAndEntityTypeIn(eq(oasysAssessmentPk), any())
    }

    @Test
    fun `should return list of associations when associations are found`() {
      val oasysAssessmentPk = "test-pk"
      val association = OasysAssociation(
        id = 1L,
        entityType = EntityType.AAP_SAN,
        oasysAssessmentPk = oasysAssessmentPk,
        entityUuid = UUID.randomUUID(),
      )
      `when`(oasysAssociationRepository.findAllByOasysAssessmentPkAndEntityTypeIn(eq(oasysAssessmentPk), any()))
        .thenReturn(listOf(association))

      val result = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

      assertEquals(1, result.size)
      assertEquals(association, result[0])
      verify(oasysAssociationRepository).findAllByOasysAssessmentPkAndEntityTypeIn(eq(oasysAssessmentPk), any())
    }
  }

  @Nested
  inner class FindAssociationsWithType {

    @Test
    fun `should return empty list when no associations are found with matching type`() {
      val oasysAssessmentPk = "test-pk"
      `when`(oasysAssociationRepository.findAllByOasysAssessmentPkAndEntityTypeIn(eq(oasysAssessmentPk), eq(listOf(EntityType.PLAN))))
        .thenReturn(emptyList())

      val result = oasysAssociationsService.findAssociationsByPkAndType(oasysAssessmentPk, listOf(EntityType.PLAN))

      assertTrue(result.isEmpty())
      verify(oasysAssociationRepository).findAllByOasysAssessmentPkAndEntityTypeIn(eq(oasysAssessmentPk), eq(listOf(EntityType.PLAN)))
    }

    @Test
    fun `should return list of associations when associations are found with type`() {
      val oasysAssessmentPk = "test-pk"
      val association = OasysAssociation(
        id = 1L,
        entityType = EntityType.AAP_SAN,
        oasysAssessmentPk = oasysAssessmentPk,
        entityUuid = UUID.randomUUID(),
      )
      `when`(oasysAssociationRepository.findAllByOasysAssessmentPkAndEntityTypeIn(oasysAssessmentPk, listOf(EntityType.AAP_SAN)))
        .thenReturn(listOf(association))

      val result = oasysAssociationsService.findAssociationsByPkAndType(oasysAssessmentPk, listOf(EntityType.AAP_SAN))

      assertEquals(1, result.size)
      assertEquals(association, result[0])
      verify(oasysAssociationRepository).findAllByOasysAssessmentPkAndEntityTypeIn(oasysAssessmentPk, listOf(EntityType.AAP_SAN))
    }
  }

  @Nested
  inner class FindAllIncludingDeleted {
    val entityUuid: UUID = UUID.randomUUID()

    @Test
    fun `should return empty list when no associations are found`() {
      `when`(oasysAssociationRepository.findAllByEntityUuidIncludingDeleted(eq(entityUuid), any()))
        .thenReturn(emptyList())

      assertTrue(oasysAssociationsService.findAllIncludingDeleted(entityUuid).isEmpty())

      verify(oasysAssociationRepository).findAllByEntityUuidIncludingDeleted(eq(entityUuid), any())
    }

    @Test
    fun `should return list of associations when associations are found`() {
      val associations = listOf(OasysAssociation())

      `when`(oasysAssociationRepository.findAllByEntityUuidIncludingDeleted(eq(entityUuid), any())).thenReturn(associations)

      assertEquals(associations, oasysAssociationsService.findAllIncludingDeleted(entityUuid))

      verify(oasysAssociationRepository).findAllByEntityUuidIncludingDeleted(eq(entityUuid), any())
    }
  }

  @Nested
  inner class FindLatestAssociationDetailsByEntityIds {
    @Test
    fun `should return empty map when no associations are found`() {
      val entityUuids = listOf(UUID.randomUUID())
      `when`(oasysAssociationRepository.findAllByEntityUuidInAndEntityTypeIn(eq(entityUuids), any())).thenReturn(emptyList())

      val result = oasysAssociationsService.findLatestAssociationDetailsByEntityIds(entityUuids)

      assertTrue(result.isEmpty())
      verify(oasysAssociationRepository).findAllByEntityUuidInAndEntityTypeIn(eq(entityUuids), any())
    }

    @Test
    fun `should return latest association details per entity UUID`() {
      val entityUuid1 = UUID.randomUUID()
      val entityUuid2 = UUID.randomUUID()
      val older = java.time.LocalDateTime.now().minusDays(2)
      val newer = java.time.LocalDateTime.now()
      val associations = listOf(
        OasysAssociation(id = 1L, entityUuid = entityUuid1, entityType = EntityType.AAP_PLAN, oasysAssessmentPk = "100", regionPrisonCode = "LDN", baseVersion = 1, createdAt = older),
        OasysAssociation(id = 2L, entityUuid = entityUuid1, entityType = EntityType.AAP_PLAN, oasysAssessmentPk = "101", regionPrisonCode = "LDN", baseVersion = 2, createdAt = newer),
        OasysAssociation(id = 3L, entityUuid = entityUuid2, entityType = EntityType.AAP_PLAN, oasysAssessmentPk = "200", regionPrisonCode = "MAN", baseVersion = 5, createdAt = newer),
      )
      `when`(oasysAssociationRepository.findAllByEntityUuidInAndEntityTypeIn(eq(listOf(entityUuid1, entityUuid2)), any())).thenReturn(associations)

      val result = oasysAssociationsService.findLatestAssociationDetailsByEntityIds(listOf(entityUuid1, entityUuid2))

      assertEquals(
        mapOf(
          entityUuid1 to EntityAssociationDetails("101", "LDN", 2),
          entityUuid2 to EntityAssociationDetails("200", "MAN", 5),
        ),
        result,
      )
    }
  }

  @Nested
  inner class FindDeletedAssociations {
    val oasysAssessmentPk = "test"

    @Test
    fun `should return empty list when no associations are found`() {
      `when`(oasysAssociationRepository.findAllDeletedByOasysAssessmentPk(eq(oasysAssessmentPk), any()))
        .thenReturn(emptyList())

      assertTrue(oasysAssociationsService.findDeletedAssociations(oasysAssessmentPk).isEmpty())

      verify(oasysAssociationRepository).findAllDeletedByOasysAssessmentPk(eq(oasysAssessmentPk), any())
    }

    @Test
    fun `should return list of associations when associations are found`() {
      val associations = listOf(OasysAssociation())

      `when`(oasysAssociationRepository.findAllDeletedByOasysAssessmentPk(eq(oasysAssessmentPk), any())).thenReturn(associations)

      assertEquals(associations, oasysAssociationsService.findDeletedAssociations(oasysAssessmentPk))

      verify(oasysAssociationRepository).findAllDeletedByOasysAssessmentPk(eq(oasysAssessmentPk), any())
    }
  }
}

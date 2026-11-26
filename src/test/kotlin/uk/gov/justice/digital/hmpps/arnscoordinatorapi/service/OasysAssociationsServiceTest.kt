package uk.gov.justice.digital.hmpps.arnscoordinatorapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.EntityAssociationDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysAssociationEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.repository.OasysAssociationRepository
import java.time.LocalDateTime
import java.util.UUID

class OasysAssociationsServiceTest {
  private val repository: OasysAssociationRepository = mock()
  private val service = OasysAssociationsService(repository)

  private val oasysAssessmentPk = "12345"

  private fun association(
    entityType: EntityType? = EntityType.AAP_PLAN,
    entityUuid: UUID = UUID.randomUUID(),
    oasysAssessmentPk: String? = this.oasysAssessmentPk,
    createdAt: LocalDateTime = LocalDateTime.now(),
    regionPrisonCode: String? = null,
    baseVersion: Long = 0,
  ) = OasysAssociationEntity(
    entityType = entityType,
    entityUuid = entityUuid,
    oasysAssessmentPk = oasysAssessmentPk,
    createdAt = createdAt,
    regionPrisonCode = regionPrisonCode,
    baseVersion = baseVersion,
  )

  @Nested
  inner class FindAssociationsByPk {
    @Test
    fun `excludes deleted associations by default`() {
      val associations = listOf(association())
      whenever(repository.findAllByOasysAssessmentPk(oasysAssessmentPk)).thenReturn(associations)

      assertEquals(associations, service.findAssociationsByPk(oasysAssessmentPk))
      verify(repository, never()).findAllByOasysAssessmentPkIncludingDeleted(oasysAssessmentPk)
    }

    @Test
    fun `includes deleted associations when asked to`() {
      val associations = listOf(association().apply { deleted = true })
      whenever(repository.findAllByOasysAssessmentPkIncludingDeleted(oasysAssessmentPk)).thenReturn(associations)

      assertEquals(associations, service.findAssociationsByPk(oasysAssessmentPk, includeDeleted = true))
      verify(repository, never()).findAllByOasysAssessmentPk(oasysAssessmentPk)
    }
  }

  @Test
  fun `findAssociationsByPkAndType delegates to the repository`() {
    val associations = listOf(association(entityType = EntityType.AAP_SAN))
    whenever(repository.findAllByOasysAssessmentPkAndEntityType(oasysAssessmentPk, EntityType.AAP_SAN)).thenReturn(associations)

    assertEquals(associations, service.findAssociationsByPkAndType(oasysAssessmentPk, EntityType.AAP_SAN))
  }

  @Test
  fun `findDeletedAssociations delegates to the repository`() {
    val associations = listOf(association().apply { deleted = true })
    whenever(repository.findAllDeletedByOasysAssessmentPk(oasysAssessmentPk)).thenReturn(associations)

    assertEquals(associations, service.findDeletedAssociations(oasysAssessmentPk))
  }

  @Test
  fun `findAllIncludingDeleted delegates to the repository`() {
    val entityUuid = UUID.randomUUID()
    val associations = listOf(association(entityUuid = entityUuid))
    whenever(repository.findAllByEntityUuidIncludingDeleted(entityUuid)).thenReturn(associations)

    assertEquals(associations, service.findAllIncludingDeleted(entityUuid))
  }

  @Nested
  inner class FindOasysPkByEntityId {
    @Test
    fun `returns the PK of the most recently created association`() {
      val entityUuid = UUID.randomUUID()
      whenever(repository.findAllByEntityUuid(entityUuid)).thenReturn(
        listOf(
          association(entityUuid = entityUuid, oasysAssessmentPk = "1", createdAt = LocalDateTime.now().minusDays(1)),
          association(entityUuid = entityUuid, oasysAssessmentPk = "2", createdAt = LocalDateTime.now()),
        ),
      )

      assertEquals("2", service.findOasysPkByEntityId(entityUuid))
    }

    @Test
    fun `returns null when there are no associations`() {
      val entityUuid = UUID.randomUUID()
      whenever(repository.findAllByEntityUuid(entityUuid)).thenReturn(emptyList())

      assertNull(service.findOasysPkByEntityId(entityUuid))
    }
  }

  @Nested
  inner class FindLatestAssociationDetailsByEntityIds {
    @Test
    fun `returns an empty map when no associations are found`() {
      val entityUuids = listOf(UUID.randomUUID())
      whenever(repository.findAllByEntityUuidIn(entityUuids)).thenReturn(emptyList())

      assertTrue(service.findLatestAssociationDetailsByEntityIds(entityUuids).isEmpty())
    }

    @Test
    fun `returns the latest association details per entity UUID`() {
      val entityUuid1 = UUID.randomUUID()
      val entityUuid2 = UUID.randomUUID()
      val older = LocalDateTime.now().minusDays(2)
      val newer = LocalDateTime.now()
      whenever(repository.findAllByEntityUuidIn(listOf(entityUuid1, entityUuid2))).thenReturn(
        listOf(
          association(entityUuid = entityUuid1, oasysAssessmentPk = "100", regionPrisonCode = "LDN", baseVersion = 1, createdAt = older),
          association(entityUuid = entityUuid1, oasysAssessmentPk = "101", regionPrisonCode = "LDN", baseVersion = 2, createdAt = newer),
          association(entityUuid = entityUuid2, oasysAssessmentPk = "200", regionPrisonCode = "MAN", baseVersion = 5, createdAt = newer),
        ),
      )

      assertEquals(
        mapOf(
          entityUuid1 to EntityAssociationDetails("101", "LDN", 2),
          entityUuid2 to EntityAssociationDetails("200", "MAN", 5),
        ),
        service.findLatestAssociationDetailsByEntityIds(listOf(entityUuid1, entityUuid2)),
      )
    }

    @Test
    fun `skips entities whose latest association has no PK`() {
      val entityUuid = UUID.randomUUID()
      whenever(repository.findAllByEntityUuidIn(listOf(entityUuid))).thenReturn(listOf(association(entityUuid = entityUuid, oasysAssessmentPk = null)))

      assertTrue(service.findLatestAssociationDetailsByEntityIds(listOf(entityUuid)).isEmpty())
    }
  }

  @Test
  fun `getExistingAssociationTypes returns distinct, non-null entity types`() {
    whenever(repository.findAllByOasysAssessmentPk(oasysAssessmentPk)).thenReturn(
      listOf(
        association(entityType = EntityType.AAP_PLAN),
        association(entityType = EntityType.AAP_PLAN),
        association(entityType = null),
        association(entityType = EntityType.AAP_SAN),
      ),
    )

    assertEquals(listOf(EntityType.AAP_PLAN, EntityType.AAP_SAN), service.getExistingAssociationTypes(oasysAssessmentPk))
  }

  @Test
  fun `save delegates to the repository`() {
    val association = association()
    whenever(repository.save(association)).thenReturn(association)

    assertEquals(association, service.save(association))
  }
}

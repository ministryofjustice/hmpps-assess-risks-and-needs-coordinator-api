package uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysAssociationEntity
import java.util.UUID

@Repository
interface OasysAssociationRepository : JpaRepository<OasysAssociationEntity, Long> {
  fun findAllByOasysAssessmentPk(oasysAssessmentPk: String): List<OasysAssociationEntity>

  fun findAllByOasysAssessmentPkAndEntityType(
    oasysAssessmentPk: String,
    entityType: EntityType,
  ): List<OasysAssociationEntity>

  fun findAllByEntityUuid(entityUuid: UUID): List<OasysAssociationEntity>

  fun findAllByEntityUuidIn(
    entityUuids: Collection<UUID>,
  ): List<OasysAssociationEntity>

  @Query(
    """
      SELECT * FROM coordinator.oasys_associations WHERE entity_uuid = :entityUuid
      """,
    nativeQuery = true,
  )
  fun findAllByEntityUuidIncludingDeleted(entityUuid: UUID): List<OasysAssociationEntity>

  @Query(
    """
      SELECT * FROM coordinator.oasys_associations WHERE oasys_assessment_pk = :oasysAssessmentPk
      """,
    nativeQuery = true,
  )
  fun findAllByOasysAssessmentPkIncludingDeleted(
    oasysAssessmentPk: String,
  ): List<OasysAssociationEntity>

  @Query(
    """
      SELECT * FROM coordinator.oasys_associations WHERE oasys_assessment_pk = :oasysAssessmentPk AND deleted IS TRUE
      """,
    nativeQuery = true,
  )
  fun findAllDeletedByOasysAssessmentPk(
    oasysAssessmentPk: String,
  ): List<OasysAssociationEntity>
}

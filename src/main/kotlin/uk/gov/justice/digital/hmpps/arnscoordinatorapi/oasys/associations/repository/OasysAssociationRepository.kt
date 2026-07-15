package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OasysAssociationRepository : JpaRepository<OasysAssociation, Long> {
  fun findAllByOasysAssessmentPkAndEntityTypeIn(
    oasysAssessmentPk: String,
    entityTypes: Collection<EntityType>,
  ): List<OasysAssociation>

  fun findAllByEntityUuidAndEntityTypeIn(entityUuid: UUID, entityTypes: Collection<EntityType>): List<OasysAssociation>

  fun findAllByEntityUuidInAndEntityTypeIn(
    entityUuids: Collection<UUID>,
    entityTypes: Collection<EntityType>,
  ): List<OasysAssociation>

  @Query(
    """
      -- Convert enum collection to names to avoid Hibernate/H2 native query enum binding issues.
      SELECT * FROM coordinator.oasys_associations WHERE entity_type in (:#{#entityTypes.![name]}) AND entity_uuid = :entityUuid
      """,
    nativeQuery = true,
  )
  fun findAllByEntityUuidIncludingDeleted(entityUuid: UUID, entityTypes: Collection<EntityType>): List<OasysAssociation>

  @Query(
    """
    -- Convert enum collection to names to avoid Hibernate/H2 native query enum binding issues.
    SELECT * FROM coordinator.oasys_associations WHERE entity_uuid = :entityUuid
    """,
    nativeQuery = true,
  )
  fun findAllOfAnyKindByEntityUuidIncludingDeleted(
    entityUuid: UUID,
    entityTypes: Collection<EntityType>,
  ): List<OasysAssociation>

  @Query(
    """
      -- Convert enum collection to names to avoid Hibernate/H2 native query enum binding issues.
      SELECT * FROM coordinator.oasys_associations WHERE entity_type in (:#{#entityTypes.![name]}) AND oasys_assessment_pk = :oasysAssessmentPk
      """,
    nativeQuery = true,
  )
  fun findAllByOasysAssessmentPkIncludingDeleted(
    oasysAssessmentPk: String,
    entityTypes: Collection<EntityType>,
  ): List<OasysAssociation>

  @Query(
    """
      -- Convert enum collection to names to avoid Hibernate/H2 native query enum binding issues.
      SELECT * FROM coordinator.oasys_associations WHERE entity_type in (:#{#entityTypes.![name]}) AND oasys_assessment_pk = :oasysAssessmentPk AND deleted IS TRUE
      """,
    nativeQuery = true,
  )
  fun findAllDeletedByOasysAssessmentPk(
    oasysAssessmentPk: String,
    entityTypes: Collection<EntityType>,
  ): List<OasysAssociation>
}

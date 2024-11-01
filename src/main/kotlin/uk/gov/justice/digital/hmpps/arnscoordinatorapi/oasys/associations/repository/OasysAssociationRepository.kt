package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OasysAssociationRepository : JpaRepository<OasysAssociation?, Long?> {
  fun findAllByOasysAssessmentPk(oasysAssessmentPk: String): List<OasysAssociation>

  @Query("SELECT * FROM coordinator.oasys_associations WHERE oasys_assessment_pk = :oasysAssessmentPk AND deleted IS TRUE", nativeQuery = true)
  fun findAllDeletedByOasysAssessmentPk(oasysAssessmentPk: String): List<OasysAssociation>

  @Query(
    """
    SELECT DISTINCT oa.oasysAssessmentPk from OasysAssociation oa where oa.entityUuid = :entityUuid
  """,
  )
  fun findOasysAssessmentPk(entityUuid: UUID): String?

  @Query("SELECT * FROM coordinator.oasys_associations WHERE entity_uuid = :entityUuid", nativeQuery = true)
  fun findAllByEntityUuidIncludingDeleted(entityUuid: UUID): List<OasysAssociation>
}

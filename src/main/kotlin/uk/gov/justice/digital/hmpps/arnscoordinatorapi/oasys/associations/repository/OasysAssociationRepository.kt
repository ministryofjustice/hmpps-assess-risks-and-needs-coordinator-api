package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OasysAssociationRepository : JpaRepository<OasysAssociation?, Long?> {
  fun findAllByOasysAssessmentPk(oasysAssessmentPk: String): List<OasysAssociation>
}

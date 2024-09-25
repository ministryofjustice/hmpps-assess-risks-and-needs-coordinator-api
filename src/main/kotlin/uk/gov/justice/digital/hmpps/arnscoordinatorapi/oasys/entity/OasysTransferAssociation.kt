package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity

import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Constraints

data class OasysTransferAssociation(
  @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
  val oldOasysAssessmentPK: String,

  @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
  val newOasysAssessmentPK: String,
)

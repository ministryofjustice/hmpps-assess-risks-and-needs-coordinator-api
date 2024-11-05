package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Constraints

data class OasysTransferAssociation(
  @field:Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
  @field:Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
  val oldOasysAssessmentPK: String,

  @field:Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
  @field:Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
  val newOasysAssessmentPK: String,
)

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Constraints
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.dto.OasysUserDetails

data class OasysMergeRequest(
  @Schema(description = "List of OASys Assessment PKs pairs to merge")
  @field:Valid
  @field:NotEmpty
  val merge: List<OasysTransferAssociation>,

  @Schema(description = "OASys User")
  @field:Valid
  val userDetails: OasysUserDetails,
)

data class OasysTransferAssociation(
  @field:Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
  @field:Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
  val oldOasysAssessmentPK: String,

  @field:Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
  @field:Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
  val newOasysAssessmentPK: String,
)

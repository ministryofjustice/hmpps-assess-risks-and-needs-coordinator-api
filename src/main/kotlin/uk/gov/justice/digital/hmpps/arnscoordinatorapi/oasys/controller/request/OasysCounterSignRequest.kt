package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.PositiveOrZero
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails

enum class Tag {
  UNSIGNED,
  LOCKED_INCOMPLETE,
  SELF_SIGNED,
  AWAITING_COUNTERSIGN,
  AWAITING_DOUBLE_COUNTERSIGN,
  COUNTERSIGNED,
  DOUBLE_COUNTERSIGNED,
  REJECTED,
  ROLLED_BACK,
  ;
}

data class OasysCounterSignRequest (
  @Schema(description = "The SAN Assessment version number that was returned from the Sign Assessment API call.", example = "2")
  @PositiveOrZero
  val sanVersionNumber: Int,

  @Schema(description = "The Sentence Plan version number that was returned from the Sign Plan API call.", example = "2")
  @PositiveOrZero
  val sentencePlanVersionNumber: Int,

  @Schema(description = "Indicates what type of case this is")
  val outcome: Tag,

  @Schema(description = "OASys User Details")
  @Valid
  val userDetails: OasysUserDetails,
)
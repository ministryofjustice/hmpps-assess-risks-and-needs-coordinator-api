package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.PositiveOrZero
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails

data class OasysRollbackRequest (
  @Schema(description = "The SAN Assessment version number that was returned from the Sign Assessment API call.", example = "2")
  @PositiveOrZero
  val sanVersionNumber: Int?,

  @Schema(description = "The plan version number that was returned from the Sign Plan API call.", example = "2")
  @PositiveOrZero
  val sentencePlanVersionNumber: Int?,

  @Schema(description = "OASys User Details")
  @Valid
  val userDetails: OasysUserDetails,
)

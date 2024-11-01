package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.PositiveOrZero
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails

data class OasysRollbackRequest(
  @Schema(description = "The SAN Assessment version number that was returned from the Sign Assessment API call.", example = "2")
  @field:PositiveOrZero
  val sanVersionNumber: Long?,

  @Schema(description = "The plan version number that was returned from the Sign Plan API call.", example = "2")
  @field:PositiveOrZero
  val sentencePlanVersionNumber: Long?,

  @Schema(description = "OASys User Details")
  @field:Valid
  val userDetails: OasysUserDetails,
)

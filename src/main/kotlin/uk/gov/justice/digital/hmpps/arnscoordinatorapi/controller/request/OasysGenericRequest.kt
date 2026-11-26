package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.dto.OasysUserDetails

data class OasysGenericRequest(
  @Schema(description = "OASys User Details")
  @field:Valid
  val userDetails: OasysUserDetails,
)

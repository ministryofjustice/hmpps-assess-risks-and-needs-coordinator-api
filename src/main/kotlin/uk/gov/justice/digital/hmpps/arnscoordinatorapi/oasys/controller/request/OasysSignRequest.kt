package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails

enum class SignType {
  SELF,
  COUNTERSIGN,
}

data class OasysSignRequest(
  @Schema(description = "Indicates what type of case this is")
  val signType: SignType,

  @Schema(description = "OASys User")
  @Valid
  val userDetails: OasysUserDetails,
)

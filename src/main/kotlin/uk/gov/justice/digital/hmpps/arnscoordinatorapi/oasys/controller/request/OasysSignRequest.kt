package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails

data class OasysSignRequest(
  @Schema(description = "Indicates the signing type")
  @Valid
  val signType: SignType,

  @Schema(description = "OASys User")
  @Valid
  val userDetails: OasysUserDetails,
)

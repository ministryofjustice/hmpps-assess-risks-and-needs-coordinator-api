package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysTransferAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails

data class OasysMergeRequest(
  @Schema(description = "List of OASys Assessment PKs pairs to merge")
  @field:Valid
  @field:NotEmpty
  val merge: List<OasysTransferAssociation>,

  @Schema(description = "OASys User")
  @field:Valid
  val userDetails: OasysUserDetails,
)

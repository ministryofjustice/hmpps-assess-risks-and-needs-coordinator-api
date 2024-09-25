package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Constraints

data class OasysUserDetails(
  @Schema(description = "User ID", example = "RB123XYZ")
  @Size(max = Constraints.OASYS_USER_ID_MAX_LENGTH)
  val id: String = "",

  @Schema(description = "User's full name", example = "John Doe")
  @Size(max = Constraints.OASYS_USER_NAME_MAX_LENGTH)
  val name: String = "",
)

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Constraints
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.Location

data class OasysUserDetails(
  @Schema(description = "User ID", example = "RB123XYZ")
  @field:Size(max = Constraints.OASYS_USER_ID_MAX_LENGTH)
  val id: String = "",

  @Schema(description = "User's full name", example = "John Doe")
  @field:Size(max = Constraints.OASYS_USER_NAME_MAX_LENGTH)
  val name: String = "",

  @Schema(description = "User's location", example = "PRISON")
  val location: Location? = null,
) {
  fun intoUserDetails() = UserDetails(id, name, UserType.OASYS, location)
}

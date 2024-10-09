package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails

data class UserDetails(
  val id: String,
  val name: String,
  val type: UserType? = null,
) {
  companion object {
    fun from(oasysUserDetails: OasysUserDetails) = UserDetails(oasysUserDetails.id, oasysUserDetails.name, UserType.OASYS)
  }
}

enum class UserType {
  OASYS,
  ARNS,
}

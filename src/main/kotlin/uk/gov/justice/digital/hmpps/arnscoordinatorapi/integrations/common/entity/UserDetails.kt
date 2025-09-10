package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.Location

data class UserDetails(
  val id: String,
  val name: String,
  val type: UserType? = null,
  var location: Location? = null,
)

enum class UserType {
  OASYS,
  ARNS,
}

enum class Location {
  PRISON,
  COMMUNITY,
}

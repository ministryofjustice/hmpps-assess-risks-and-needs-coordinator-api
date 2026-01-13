package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

data class UserDetails(
  val id: String,
  val name: String,
  val type: UserType? = null,
)

enum class UserType {
  OASYS,
  ARNS,
}

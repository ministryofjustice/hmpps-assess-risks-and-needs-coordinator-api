package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

enum class SignType {
  SELF,
  COUNTERSIGN,
}

data class SignData(
  val signType: SignType,
  val userDetails: UserDetails,
)

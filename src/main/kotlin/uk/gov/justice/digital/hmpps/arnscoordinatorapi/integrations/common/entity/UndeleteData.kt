package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

data class UndeleteData(
  val userDetails: UserDetails,
  val versionFrom: Long,
  val versionTo: Long? = null,
)

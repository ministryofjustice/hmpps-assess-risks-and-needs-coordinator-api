package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails

data class RollbackData(
  val versionNumber: Long,
  val userDetails: UserDetails,
)

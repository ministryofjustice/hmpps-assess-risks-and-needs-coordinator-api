package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UndeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails

data class UndeletePlanData(
  val userDetails: UserDetails,
  val from: Long,
  val to: Long? = null,
) {
  companion object {
    fun from(undeleteData: UndeleteData) = with(undeleteData) {
      UndeletePlanData(userDetails, versionFrom, versionTo)
    }
  }
}

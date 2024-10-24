package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SoftDeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails

data class SoftDeletePlanData(
  val userDetails: UserDetails,
  val from: Long,
  val to: Long? = null,
) {
  companion object {
    fun from(softDeleteData: SoftDeleteData) = with(softDeleteData) {
      SoftDeletePlanData(userDetails, versionFrom, versionTo)
    }
  }
}

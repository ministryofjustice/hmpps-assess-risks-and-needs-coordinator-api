package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest

data class PlanVersionData(
  val sentencePlanVersion: Long,
  val userDetails: UserDetails,
) {
  companion object {
    fun from(request: OasysRollbackRequest) = request.sentencePlanVersionNumber
      ?.let { PlanVersionData(it, request.userDetails.intoUserDetails()) }
      ?: throw Exception("Unable to construct rollback request data. Missing sentencePlanVersionNumber.")
  }
}

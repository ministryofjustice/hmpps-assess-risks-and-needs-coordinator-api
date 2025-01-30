package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest

data class RollbackData(
  val versionNumber: Long,
  val userDetails: UserDetails,
) {
  companion object {
    fun from(request: OasysRollbackRequest) = request.sanVersionNumber
      ?.let { RollbackData(it, request.userDetails.intoUserDetails()) }
      ?: throw Exception("Unable to construct rollback request data. Missing sanVersionNumber.")
  }
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest

data class CounterSignAssessmentData(
  val versionNumber: Long,
  val outcome: CounterSignOutcome,
  val userDetails: UserDetails,
) {
  companion object {
    fun from(request: OasysCounterSignRequest) = with(request) {
      sanVersionNumber?.let { versionNumber ->
        CounterSignAssessmentData(
          versionNumber,
          outcome,
          userDetails.intoUserDetails(),
        )
      } ?: throw throw Exception("Unable to construct counter-sign request data. Missing sanVersionNumber")
    }
  }
}

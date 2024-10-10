package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest

data class CounterSignPlanData(
  val sentencePlanVersion: Long,
  val signType: CounterSignOutcome,
) {
  companion object {
    fun from(request: OasysCounterSignRequest) = with(request) {
      sentencePlanVersionNumber?.let { versionNumber ->
        CounterSignPlanData(
          versionNumber,
          outcome,
        )
      } ?: throw throw Exception("Unable to construct counter-sign request data. Missing sentencePlanVersionNumber")
    }
  }
}

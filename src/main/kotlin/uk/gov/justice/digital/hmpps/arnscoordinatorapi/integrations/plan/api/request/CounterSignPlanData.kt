package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest

data class CounterSignPlanData(
  val sentencePlanVersion: Int,
  val signType: CounterSignOutcome,
) {
  companion object {
    fun from(request: OasysCounterSignRequest) = with(request) {
      CounterSignPlanData(
        sentencePlanVersionNumber,
        outcome,
      )
    }
  }
}

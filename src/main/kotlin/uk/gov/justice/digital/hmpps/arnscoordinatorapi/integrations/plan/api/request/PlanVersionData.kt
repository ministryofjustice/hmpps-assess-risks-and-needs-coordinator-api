package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest

data class PlanVersionData(
  val sentencePlanVersion: Long,
) {
  companion object {
    fun from(request: OasysRollbackRequest) =
      request.sentencePlanVersionNumber
        ?.let { PlanVersionData(it) }
        ?: throw Exception("Unable to construct rollback request data. Missing sentencePlanVersionNumber.")
  }
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.DeleteAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.DeletePlanData

data class DeleteData(
  val plan: DeletePlanData? = null,
  val assessment: DeleteAssessmentData? = null,
) {
  companion object {
    fun from(createData: CreateData) = DeleteData(
      plan = createData.plan?.let { DeletePlanData(it.userDetails) },
      assessment = createData.assessment?.let { DeleteAssessmentData(it.userDetails) },
    )
  }
}

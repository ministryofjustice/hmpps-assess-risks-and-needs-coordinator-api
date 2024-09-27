package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response

import java.util.*

data class CreatePlanResponse(
  val sentencePlanId: UUID,
  val sentencePlanVersion: Long,
)

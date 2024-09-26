package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateResponse
import java.util.*

data class CreatePlanResponse(
  val sentencePlanId: UUID,
  val sentencePlanVersion: Long,
) : CreateResponse

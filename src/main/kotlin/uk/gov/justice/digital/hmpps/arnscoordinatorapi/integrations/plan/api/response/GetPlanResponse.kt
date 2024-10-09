package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanState
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import java.time.LocalDateTime
import java.util.UUID

data class GetPlanResponse(
  var sentencePlanId: UUID,
  var sentencePlanVersion: Long,
  var planComplete: PlanState,
  var planType: PlanType,
  var lastUpdatedTimestampSP: LocalDateTime,
)

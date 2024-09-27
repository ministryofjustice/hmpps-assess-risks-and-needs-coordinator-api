package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType

data class CreatePlanData(
  val planType: PlanType,
  val userDetails: UserDetails,
)

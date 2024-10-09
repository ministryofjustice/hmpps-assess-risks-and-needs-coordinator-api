package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response

import java.util.UUID

class PlanVersionResponse(
  val planId: UUID,
  val planVersion: Long,
)

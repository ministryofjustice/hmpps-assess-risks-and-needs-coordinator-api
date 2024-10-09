package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response

import com.fasterxml.jackson.annotation.JsonAlias
import java.util.UUID

data class CreatePlanResponse(
  @JsonAlias("planId")
  val sentencePlanId: UUID,
  @JsonAlias("planVersion")
  val sentencePlanVersion: Long,
)

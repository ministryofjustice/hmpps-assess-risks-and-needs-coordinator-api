package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request

import java.util.UUID

data class ResetPlanRequest(
  val user: AAPUser,
  val assessmentUuid: UUID
)
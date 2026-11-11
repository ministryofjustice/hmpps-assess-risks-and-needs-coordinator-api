package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.SanAssessmentType
import java.util.UUID

data class StartNewPathwayRequest(
  val user: AAPUser,
  val assessmentUuid: UUID,
  val previousPathway: SanAssessmentType,
  val newPathway: SanAssessmentType,
)

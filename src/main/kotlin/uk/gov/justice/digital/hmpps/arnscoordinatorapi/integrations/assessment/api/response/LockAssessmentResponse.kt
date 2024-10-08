package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response

import java.util.UUID

data class LockAssessmentResponse(
  val sanAssessmentId: UUID,
  val sanAssessmentVersion: Long,
)

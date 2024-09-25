package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response

import java.util.*

data class CreateAssessmentResponse(
  val sanAssessmentId: UUID,
  val sanAssessmentVersion: Long,
)

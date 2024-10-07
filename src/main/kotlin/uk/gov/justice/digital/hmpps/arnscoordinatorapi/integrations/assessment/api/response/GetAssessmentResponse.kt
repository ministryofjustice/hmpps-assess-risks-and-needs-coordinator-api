package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response

import java.time.LocalDateTime
import java.util.UUID

data class GetAssessmentResponse(
  val sanAssessmentId: UUID,
  val sanAssessmentVersion: Long,
  val sanAssessmentData: Map<String, *>,
  val lastUpdatedTimestamp: LocalDateTime,
)

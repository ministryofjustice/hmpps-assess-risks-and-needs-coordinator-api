package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response

import com.fasterxml.jackson.annotation.JsonAlias
import java.util.UUID

data class CreateAssessmentResponse(
  @JsonAlias("id")
  val sanAssessmentId: UUID,
  @JsonAlias("version")
  val sanAssessmentVersion: Long,
)

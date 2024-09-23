package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response

import java.util.*

data class OasysCreateResponse (
  val sanAssessmentId: UUID,
  val sanAssessmentVersion: Int,
  val sentencePlanId: UUID? = null,
  val sentencePlanVersion: Int? = null,
)

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response

import java.util.UUID

class OasysVersionedEntityResponse(
  val sanAssessmentId: UUID,
  val sanAssessmentVersion: Int,
  val sentencePlanId: UUID,
  val sentencePlanVersion: Int,
)

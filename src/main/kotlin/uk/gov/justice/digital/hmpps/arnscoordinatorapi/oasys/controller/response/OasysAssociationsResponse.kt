package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response

import java.util.UUID

data class OasysAssociationsResponse(
  val sanAssessmentId: UUID,
  val sentencePlanId: UUID,
)

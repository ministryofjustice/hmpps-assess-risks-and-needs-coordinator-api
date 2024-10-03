package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response

import java.util.UUID

data class OasysAssociationsResponse(
  var sanAssessmentId: UUID? = null,
  var sentencePlanId: UUID? = null,
)

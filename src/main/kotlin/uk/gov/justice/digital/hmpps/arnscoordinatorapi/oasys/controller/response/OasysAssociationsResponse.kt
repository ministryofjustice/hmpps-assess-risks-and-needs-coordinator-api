package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response

import java.util.UUID

data class OasysAssociationsResponse(
  var sanAssessmentId: UUID = UUID(0, 0),
  var sentencePlanId: UUID = UUID(0, 0),
)

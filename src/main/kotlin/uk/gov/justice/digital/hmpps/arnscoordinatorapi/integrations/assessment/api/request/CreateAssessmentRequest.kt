package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails

data class CreateAssessmentRequest (
  val userDetails: OasysUserDetails
)

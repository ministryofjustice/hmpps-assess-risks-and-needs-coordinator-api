package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.SanAssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.oasysdata.OasysData

data class CreateAssessmentData(
  val userDetails: UserDetails,

  // Both fields are used to build AAP properties and won't be sent to the old SAN (Strengths and needs api)
  @JsonIgnore
  val sanAssessmentType: SanAssessmentType? = null,

  @JsonIgnore
  val oasysData: OasysData? = null,
)

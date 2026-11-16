package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.SanAssessmentType

data class CreateAssessmentData(
  val userDetails: UserDetails,

  // Both fields are only sent to AAP (as properties and the PrepopulateFromOasys hook) and won't be sent to the old SAN (Strengths and needs api)
  @JsonIgnore
  val sanAssessmentType: SanAssessmentType? = null,

  @JsonIgnore
  val oasysData: Map<String, Any?>? = null,
)

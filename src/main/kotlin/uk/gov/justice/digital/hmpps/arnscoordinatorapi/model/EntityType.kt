package uk.gov.justice.digital.hmpps.arnscoordinatorapi.model

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.CoordinatorException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.AAPAssessmentType.SENTENCE_PLAN
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.AAPAssessmentType.STRENGTHS_AND_NEEDS

enum class EntityType {
  AAP_PLAN,
  AAP_SAN,
  ;

  companion object {
    fun fromAAPAssessmentType(type: String) = when (type) {
      STRENGTHS_AND_NEEDS.name -> AAP_SAN
      SENTENCE_PLAN.name -> AAP_PLAN
      else -> throw CoordinatorException("Unexpected AAP Assessment Type $type")
    }
  }
}

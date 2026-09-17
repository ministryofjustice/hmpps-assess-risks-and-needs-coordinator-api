package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import kotlin.collections.setOf

@Component
class AssessmentTypeConfig(
  @Value("\${plan-type}")
  val planType: EntityType,

  @Value("\${assessment-type}")
  val assessmentType: EntityType,
) {
  fun enabledEntityTypes() = setOf(assessmentType, planType)

  fun getEntityTypesFor(assessmentType: AssessmentType) = when (assessmentType) {
    AssessmentType.SAN_SP -> enabledEntityTypes()
    AssessmentType.SP -> setOf(planType)
  }
}

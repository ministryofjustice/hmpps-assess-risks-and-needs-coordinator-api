package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentTypeConfig

@Component
class StrategyFactory(
  private val assessmentStrategy: AssessmentStrategy?,
  private val planStrategy: PlanStrategy?,
  private val aapPlanStrategy: AAPPlanStrategy?,
  private val aapStrengthsAndNeedsStrategy: AAPStrengthsAndNeedsStrategy?,
  private val assessmentTypeConfig: AssessmentTypeConfig,
) {

  fun getStrategy(entityType: EntityType): EntityStrategy = when (entityType) {
    EntityType.ASSESSMENT -> assessmentStrategy ?: throw IllegalStateException("Assessment strategy is disabled")
    EntityType.PLAN -> planStrategy ?: throw IllegalStateException("Plan strategy is disabled")
    EntityType.AAP_PLAN -> aapPlanStrategy ?: throw IllegalStateException("AAP Plan strategy is disabled")
    EntityType.AAP_SAN -> aapStrengthsAndNeedsStrategy ?: throw IllegalStateException("AAP StrengthsAndNeedsStrategy is disabled")
  }

  fun getStrategies(): List<EntityStrategy> = listOfNotNull(
    assessmentStrategy,
    planStrategy,
    aapPlanStrategy,
    aapStrengthsAndNeedsStrategy,
  )

  fun getStrategiesFor(assessmentType: AssessmentType): List<EntityStrategy> = getStrategies()
    .filter { it.entityType in assessmentTypeConfig.getEntityTypesFor(assessmentType) }
}

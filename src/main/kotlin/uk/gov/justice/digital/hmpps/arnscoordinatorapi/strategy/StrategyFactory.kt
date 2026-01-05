package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType

@Component
class StrategyFactory(
  private val assessmentStrategy: AssessmentStrategy?,
  private val planStrategy: PlanStrategy?,
  private val aapPlanStrategy: AAPPlanStrategy?,
) {

  fun getStrategy(entityType: EntityType): EntityStrategy = when (entityType) {
    EntityType.ASSESSMENT -> assessmentStrategy ?: throw IllegalStateException("Assessment strategy is disabled")
    EntityType.PLAN -> planStrategy ?: throw IllegalStateException("Plan strategy is disabled")
    EntityType.AAP_PLAN -> aapPlanStrategy ?: throw IllegalStateException("AAP Plan strategy is disabled")
  }

  fun getStrategies(): List<EntityStrategy> = listOfNotNull(
    assessmentStrategy,
    planStrategy,
  )

  fun getStrategiesForCreate(planType: PlanType): List<EntityStrategy> = when (planType) {
    PlanType.PLAN_ONLY -> listOfNotNull(aapPlanStrategy)
    else -> getStrategies()
  }
}

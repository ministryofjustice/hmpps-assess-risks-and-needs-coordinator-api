package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType

@Component
class StrategyFactory(
  private val assessmentStrategy: AssessmentStrategy?,
  private val planStrategy: PlanStrategy?,
) {

  fun getStrategy(entityType: EntityType): EntityStrategy {
    return when (entityType) {
      EntityType.ASSESSMENT -> assessmentStrategy ?: throw IllegalStateException("Assessment strategy is disabled")
      EntityType.PLAN -> planStrategy ?: throw IllegalStateException("Plan strategy is disabled")
    }
  }

  fun getStrategies(): List<EntityStrategy> {
    return listOfNotNull(
      assessmentStrategy,
      planStrategy,
    )
  }
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentTypeConfig

class StrategyFactoryTest {
  private val assessmentStrategy: AssessmentStrategy = mock()
  private val planStrategy: PlanStrategy = mock()
  private val aapPlanStrategy: AAPPlanStrategy = mock()
  private val aapStrengthsAndNeedsStrategy: AAPStrengthsAndNeedsStrategy = mock()
  private val assessmentTypeConfig: AssessmentTypeConfig = mock()

  private lateinit var entityStrategyFactory: StrategyFactory

  @BeforeEach
  fun setUp() {
    whenever(assessmentStrategy.entityType).thenReturn(EntityType.ASSESSMENT)
    whenever(planStrategy.entityType).thenReturn(EntityType.PLAN)
    whenever(aapPlanStrategy.entityType).thenReturn(EntityType.AAP_PLAN)
    whenever(aapStrengthsAndNeedsStrategy.entityType).thenReturn(EntityType.AAP_SAN)
    whenever { assessmentTypeConfig.getEntityTypesFor(AssessmentType.SAN_SP) }.thenReturn(
      setOf(
        EntityType.AAP_SAN,
        EntityType.AAP_PLAN,
      ),
    )
    whenever { assessmentTypeConfig.getEntityTypesFor(AssessmentType.SP) }.thenReturn(setOf(EntityType.AAP_PLAN))
    entityStrategyFactory = StrategyFactory(assessmentStrategy, planStrategy, aapPlanStrategy, aapStrengthsAndNeedsStrategy, assessmentTypeConfig)
  }

  @Nested
  inner class GetStrategy {

    @Test
    fun `should return AssessmentStrategy when entityType is ASSESSMENT`() {
      val result = entityStrategyFactory.getStrategy(EntityType.ASSESSMENT)

      assertSame(assessmentStrategy, result)
    }

    @Test
    fun `should return PlanStrategy when entityType is PLAN`() {
      val result = entityStrategyFactory.getStrategy(EntityType.PLAN)

      assertSame(planStrategy, result)
    }

    @Test
    fun `should return AAPPlanStrategy when entityType is AAP_PLAN`() {
      val result = entityStrategyFactory.getStrategy(EntityType.AAP_PLAN)

      assertSame(aapPlanStrategy, result)
    }

    @Test
    fun `should return AAPStrengthsAndNeedsStrategy when entityType is AAP_SAN`() {
      val result = entityStrategyFactory.getStrategy(EntityType.AAP_SAN)

      assertSame(aapStrengthsAndNeedsStrategy, result)
    }
  }

  @Nested
  inner class GetStrategiesFor {

    @Test
    fun `should return AAP_PLAN and AAP_SAN strategies for SAN_SP`() {
      val result = entityStrategyFactory.getStrategiesFor(AssessmentType.SAN_SP)

      assertTrue(result.contains(aapPlanStrategy))
      assertTrue(result.contains(aapStrengthsAndNeedsStrategy))
    }

    @Test
    fun `should return only AAP_PLAN strategy for SP`() {
      val result = entityStrategyFactory.getStrategiesFor(AssessmentType.SP)

      assertTrue(result.contains(aapPlanStrategy))
      assertTrue(!result.contains(aapStrengthsAndNeedsStrategy))
    }
  }
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType

class StrategyFactoryTest {
  private val assessmentStrategy: AssessmentStrategy = mock()
  private val planStrategy: PlanStrategy = mock()
  private val aapPlanStrategy: AAPPlanStrategy = mock()

  private lateinit var entityStrategyFactory: StrategyFactory

  @BeforeEach
  fun setUp() {
    entityStrategyFactory = StrategyFactory(assessmentStrategy, planStrategy, aapPlanStrategy)
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
  }

  @Nested
  inner class GetStrategiesForCreate {

    @Test
    fun `should return AAPPlanStrategy when planType is PLAN_ONLY`() {
      val result = entityStrategyFactory.getStrategiesForCreate(PlanType.PLAN_ONLY)

      assertEquals(1, result.size)
      assertSame(aapPlanStrategy, result.first())
    }

    @Test
    fun `should return existing strategies when planType is INITIAL`() {
      val result = entityStrategyFactory.getStrategiesForCreate(PlanType.INITIAL)

      assertEquals(2, result.size)
      assertTrue(result.contains(assessmentStrategy))
      assertTrue(result.contains(planStrategy))
    }

    @Test
    fun `should return existing strategies when planType is REVIEW`() {
      val result = entityStrategyFactory.getStrategiesForCreate(PlanType.REVIEW)

      assertEquals(2, result.size)
      assertTrue(result.contains(assessmentStrategy))
      assertTrue(result.contains(planStrategy))
    }
  }
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentType

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
  }

  @Nested
  inner class GetStrategiesForWithAssessmentDisabled {
    private val factory = StrategyFactory(null, planStrategy, aapPlanStrategy)

    @BeforeEach
    fun setUp() {
      whenever(planStrategy.entityType).thenReturn(EntityType.PLAN)
      whenever(aapPlanStrategy.entityType).thenReturn(EntityType.AAP_PLAN)
    }

    @Test
    fun `should return only the AAP plan strategy for SAN_SP`() {
      assertEquals(listOf(aapPlanStrategy), factory.getStrategiesFor(AssessmentType.SAN_SP))
    }

    @Test
    fun `should return only the AAP plan strategy for SP`() {
      assertEquals(listOf(aapPlanStrategy), factory.getStrategiesFor(AssessmentType.SP))
    }
  }
}

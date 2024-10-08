package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.SentencePlanApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.GetPlanResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanState
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.time.LocalDateTime
import java.util.UUID

class PlanStrategyTest {
  private val sentencePlanApi: SentencePlanApi = mock()
  private lateinit var planStrategy: PlanStrategy

  @BeforeEach
  fun setup() {
    planStrategy = PlanStrategy(sentencePlanApi)
  }

  @Nested
  inner class Create {

    @Test
    fun `should return success when create plan is successful`() {
      val createData = CreateData(
        plan = CreatePlanData(PlanType.INITIAL, UserDetails("id", "name")),
      )
      val versionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.PLAN)

      `when`(sentencePlanApi.createPlan(any())).thenReturn(
        SentencePlanApi.ApiOperationResult.Success(versionedEntity),
      )

      val result = planStrategy.create(createData)

      assertTrue(result is OperationResult.Success)
      assertEquals(versionedEntity, (result as OperationResult.Success).data)
      verify(sentencePlanApi).createPlan(any())
    }

    @Test
    fun `should return failure when create plan fails`() {
      val createData = CreateData(
        plan = CreatePlanData(PlanType.INITIAL, UserDetails("id", "name")),
      )

      `when`(sentencePlanApi.createPlan(any())).thenReturn(
        SentencePlanApi.ApiOperationResult.Failure("Error occurred"),
      )

      val result = planStrategy.create(createData)

      assertTrue(result is OperationResult.Failure)
      assertEquals("Error occurred", (result as OperationResult.Failure).errorMessage)
      verify(sentencePlanApi).createPlan(any())
    }
  }

  @Nested
  inner class Fetch {

    @Test
    fun `should return success when fetch plan is successful`() {
      val entityUuid = UUID.randomUUID()
      val getPlanResponse = GetPlanResponse(
        sentencePlanId = entityUuid,
        sentencePlanVersion = 1,
        planComplete = PlanState.INCOMPLETE,
        planType = PlanType.INITIAL,
        lastUpdatedTimestampSP = LocalDateTime.now(),
      )

      `when`(sentencePlanApi.getPlan(entityUuid)).thenReturn(
        SentencePlanApi.ApiOperationResult.Success(getPlanResponse),
      )

      val result = planStrategy.fetch(entityUuid)

      assertTrue(result is OperationResult.Success)
      assertEquals(getPlanResponse, (result as OperationResult.Success).data)
      verify(sentencePlanApi).getPlan(entityUuid)
    }

    @Test
    fun `should return failure when fetch plan fails`() {
      val entityUuid = UUID.randomUUID()

      `when`(sentencePlanApi.getPlan(entityUuid)).thenReturn(
        SentencePlanApi.ApiOperationResult.Failure("Fetch error occurred"),
      )

      val result = planStrategy.fetch(entityUuid)

      assertTrue(result is OperationResult.Failure)
      assertEquals("Fetch error occurred", (result as OperationResult.Failure).errorMessage)
      verify(sentencePlanApi).getPlan(entityUuid)
    }
  }
}

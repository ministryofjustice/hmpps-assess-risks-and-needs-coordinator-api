package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.SentencePlanApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

class PlanServiceTest {
  private val sentencePlanApi = mock(SentencePlanApi::class.java)
  private val planService = PlanService(sentencePlanApi)
  private val userDetails = UserDetails(id = "user123", name = "John Doe")

  @Nested
  inner class Create {

    @Test
    fun `should return success when SentencePlanApi returns success`() {
      val createPlanData = CreatePlanData(
        userDetails = userDetails,
        planType = PlanType.INITIAL,
      )
      val createData = CreateData(
        plan = createPlanData,
      )
      val versionedEntity = VersionedEntity(
        id = UUID.randomUUID(),
        version = 1L,
        entityType = EntityType.PLAN,
      )
      val successResult = SentencePlanApi.ApiOperationResult.Success(versionedEntity)

      `when`(sentencePlanApi.createPlan(createPlanData)).thenReturn(successResult)

      val result = planService.create(createData)

      assertTrue(result is OperationResult.Success)
      assertEquals(versionedEntity, (result as OperationResult.Success).data)

      verify(sentencePlanApi).createPlan(createPlanData)
    }

    @Test
    fun `should return failure when SentencePlanApi returns failure`() {
      val createPlanData = CreatePlanData(
        userDetails = userDetails,
        planType = PlanType.INITIAL,
      )
      val createData = CreateData(
        plan = createPlanData,
      )
      val failureResult = SentencePlanApi.ApiOperationResult.Failure<Nothing>("Failed to create plan")

      `when`(sentencePlanApi.createPlan(createPlanData)).thenReturn(failureResult)

      val result = planService.create(createData)

      assertTrue(result is OperationResult.Failure)
      assertEquals("Failed to create plan", (result as OperationResult.Failure).errorMessage)

      verify(sentencePlanApi).createPlan(createPlanData)
    }
  }
}

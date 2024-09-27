package uk.gov.justice.digital.hmpps.arnscoordinatorapi.features

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.IntegrationService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

class ActionServiceTest {
  private val component1 = mock(IntegrationService::class.java)
  private val component2 = mock(IntegrationService::class.java)
  private val actionService = ActionService(listOf(component1, component2))

  private val userDetails = UserDetails(id = "user123", name = "John Doe")
  private val planData = CreatePlanData(userDetails = userDetails, planType = PlanType.INITIAL)
  private val assessmentData = CreateAssessmentData(userDetails = userDetails)

  @Nested
  inner class CreateAllEntities {

    @Test
    fun `should return success when all components return success`() {
      val createData = CreateData(plan = planData, assessment = assessmentData)

      val versionedEntity1 = VersionedEntity(UUID.randomUUID(), 0L, EntityType.PLAN)
      val versionedEntity2 = VersionedEntity(UUID.randomUUID(), 0L, EntityType.ASSESSMENT)

      `when`(component1.create(createData)).thenReturn(OperationResult.Success(versionedEntity1))
      `when`(component2.create(createData)).thenReturn(OperationResult.Success(versionedEntity2))

      val result = actionService.createAllEntities(createData)

      assertTrue(result is OperationResult.Success)
      val entities = (result as OperationResult.Success).data
      assertEquals(2, entities.size)
      assertTrue(entities.contains(versionedEntity1))
      assertTrue(entities.contains(versionedEntity2))

      verify(component1).create(createData)
      verify(component2).create(createData)
    }

    @Test
    fun `should return failure when any component returns failure`() {
      val createData = CreateData(plan = planData, assessment = assessmentData)
      val versionedEntity1 = VersionedEntity(UUID.randomUUID(), 1L, EntityType.PLAN)
      val errorMessage = "Component 2 failed"

      `when`(component1.create(createData)).thenReturn(OperationResult.Success(versionedEntity1))
      `when`(component2.create(createData)).thenReturn(OperationResult.Failure(errorMessage))

      val result = actionService.createAllEntities(createData)

      assertTrue(result is OperationResult.Failure)
      assertEquals(errorMessage, (result as OperationResult.Failure).errorMessage)

      verify(component1).create(createData)
      verify(component2).create(createData)
    }

    @Test
    fun `should return failure when all components return failure`() {
      val createData = CreateData(plan = planData, assessment = assessmentData)
      val errorMessageComponent1 = "Component 1 failed"
      val errorMessageComponent2 = "Component 2 failed"

      `when`(component1.create(createData)).thenReturn(OperationResult.Failure(errorMessageComponent1))
      `when`(component2.create(createData)).thenReturn(OperationResult.Failure(errorMessageComponent2))

      val result = actionService.createAllEntities(createData)

      assertTrue(result is OperationResult.Failure)
      (result as OperationResult.Failure)
      assertTrue(result.errorMessage.contains(errorMessageComponent1))
      assertTrue(result.errorMessage.contains(errorMessageComponent2))

      verify(component1).create(createData)
      verify(component2).create(createData)
    }
  }
}

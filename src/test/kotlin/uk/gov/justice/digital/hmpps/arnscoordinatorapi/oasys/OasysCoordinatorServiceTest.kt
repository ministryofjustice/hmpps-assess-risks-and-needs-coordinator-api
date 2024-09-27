package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.features.ActionService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.util.UUID

class OasysCoordinatorServiceTest {
  private val actionService = mock(ActionService::class.java)
  private val oasysAssociationsService = mock(OasysAssociationsService::class.java)
  private val oasysCoordinatorService = OasysCoordinatorService(actionService, oasysAssociationsService)

  val requestData = OasysCreateRequest(
    oasysAssessmentPk = "XY/12456",
    planType = PlanType.INITIAL,
    regionPrisonCode = "123",
    userDetails = OasysUserDetails(id = "user123", name = "John Doe"),
  )

  @Nested
  inner class Create {

    @Test
    fun `should return success when all services succeed`() {
      val versionedEntityPlan = VersionedEntity(UUID.randomUUID(), 1L, EntityType.PLAN)
      val versionedEntityAssessment = VersionedEntity(UUID.randomUUID(), 2L, EntityType.ASSESSMENT)

      `when`(oasysAssociationsService.ensureNoExistingAssociation(requestData.oasysAssessmentPk))
        .thenReturn(OperationResult.Success(Unit))
      `when`(actionService.createAllEntities(any<CreateData>()))
        .thenReturn(OperationResult.Success(listOf(versionedEntityPlan, versionedEntityAssessment)))
      `when`(oasysAssociationsService.storeAssociation(any<OasysAssociation>()))
        .thenReturn(OperationResult.Success(Unit))

      val result = oasysCoordinatorService.create(requestData)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)
      val response = (result as OasysCoordinatorService.CreateOperationResult.Success).data

      assertEquals(versionedEntityPlan.id, response.sentencePlanId)
      assertEquals(versionedEntityPlan.version, response.sentencePlanVersion)
      assertEquals(versionedEntityAssessment.id, response.sanAssessmentId)
      assertEquals(versionedEntityAssessment.version, response.sanAssessmentVersion)

      verify(oasysAssociationsService).ensureNoExistingAssociation(requestData.oasysAssessmentPk)
      verify(actionService).createAllEntities(any<CreateData>())
      verify(oasysAssociationsService, times(2)).storeAssociation(any<OasysAssociation>())
    }

    @Test
    fun `should return failure when actionService fails to create entities`() {
      `when`(oasysAssociationsService.ensureNoExistingAssociation(requestData.oasysAssessmentPk))
        .thenReturn(OperationResult.Success(Unit))
      `when`(actionService.createAllEntities(any<CreateData>()))
        .thenReturn(OperationResult.Failure("Failed to create entities"))

      val result = oasysCoordinatorService.create(requestData)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Failure)
      val failureMessage = (result as OasysCoordinatorService.CreateOperationResult.Failure).errorMessage
      assertEquals("Cannot create, creating entities failed: Failed to create entities", failureMessage)

      verify(oasysAssociationsService).ensureNoExistingAssociation(requestData.oasysAssessmentPk)
      verify(actionService).createAllEntities(any())
    }

    @Test
    fun `should return failure due to conflicting associations`() {
      `when`(oasysAssociationsService.ensureNoExistingAssociation(requestData.oasysAssessmentPk))
        .thenReturn(OperationResult.Failure("Conflicting association"))

      val result = oasysCoordinatorService.create(requestData)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.ConflictingAssociations)
      val failureMessage = (result as OasysCoordinatorService.CreateOperationResult.ConflictingAssociations).errorMessage
      assertEquals("Cannot create due to conflicting associations: Conflicting association", failureMessage)

      verify(oasysAssociationsService).ensureNoExistingAssociation(requestData.oasysAssessmentPk)
      verifyNoMoreInteractions(actionService) // Ensure actionService is not called
    }

    @Test
    fun `should return failure when association storage fails after entity creation`() {
      val versionedEntityPlan = VersionedEntity(UUID.randomUUID(), 1L, EntityType.PLAN)
      val versionedEntityAssessment = VersionedEntity(UUID.randomUUID(), 2L, EntityType.ASSESSMENT)

      `when`(oasysAssociationsService.ensureNoExistingAssociation(requestData.oasysAssessmentPk))
        .thenReturn(OperationResult.Success(Unit))
      `when`(actionService.createAllEntities(any<CreateData>()))
        .thenReturn(OperationResult.Success(listOf(versionedEntityPlan, versionedEntityAssessment)))
      `when`(oasysAssociationsService.storeAssociation(any<OasysAssociation>()))
        .thenReturn(OperationResult.Failure("Failed to store association"))

      val result = oasysCoordinatorService.create(requestData)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Failure)
      val failureMessage = (result as OasysCoordinatorService.CreateOperationResult.Failure).errorMessage
      assertEquals("Failed saving associations: Failed to store association", failureMessage)

      verify(oasysAssociationsService).ensureNoExistingAssociation(requestData.oasysAssessmentPk)
      verify(actionService).createAllEntities(any<CreateData>())
      verify(oasysAssociationsService).storeAssociation(any<OasysAssociation>())
    }
  }
}

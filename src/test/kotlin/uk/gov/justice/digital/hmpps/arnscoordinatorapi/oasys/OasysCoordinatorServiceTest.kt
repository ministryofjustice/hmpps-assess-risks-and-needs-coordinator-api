package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.StrategyFactory
import java.util.UUID

class OasysCoordinatorServiceTest {
  private val strategyFactory: StrategyFactory = mock()
  private val oasysAssociationsService: OasysAssociationsService = mock()

  private lateinit var oasysCoordinatorService: OasysCoordinatorService

  private val oasysCreateRequest = OasysCreateRequest(
    oasysAssessmentPk = "CY/12ZX56",
    regionPrisonCode = "111111",
    planType = PlanType.INITIAL,
    userDetails = OasysUserDetails(id = "userId", name = "John Doe"),
  )

  private val versionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.PLAN)

  @BeforeEach
  fun setup() {
    oasysCoordinatorService = OasysCoordinatorService(strategyFactory, oasysAssociationsService)
  }

  @Nested
  inner class Create {

    @Test
    fun `should create entities and associations successfully`() {
      val strategy: EntityStrategy = mock { on { entityType } doReturn EntityType.PLAN }

      `when`(oasysAssociationsService.ensureNoExistingAssociation(anyString()))
        .thenReturn(OperationResult.Success(Unit))

      `when`(strategyFactory.getStrategies()).thenReturn(listOf(strategy))

      `when`(strategy.create(any())).thenReturn(OperationResult.Success(versionedEntity))

      `when`(oasysAssociationsService.storeAssociation(any())).thenReturn(OperationResult.Success(Unit))

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)
      val response = (result as OasysCoordinatorService.CreateOperationResult.Success).data
      assertEquals(versionedEntity.id, response.sentencePlanId)

      verify(strategyFactory).getStrategies()
      verify(oasysAssociationsService).ensureNoExistingAssociation(oasysCreateRequest.oasysAssessmentPk)
      verify(oasysAssociationsService, times(1)).storeAssociation(any())
    }

    @Test
    fun `should create entities and associations successfully for both PLAN and ASSESSMENT`() {
      val planStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.PLAN }
      val assessmentStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }

      `when`(oasysAssociationsService.ensureNoExistingAssociation(anyString()))
        .thenReturn(OperationResult.Success(Unit))

      `when`(strategyFactory.getStrategies()).thenReturn(listOf(planStrategy, assessmentStrategy))

      `when`(planStrategy.create(any())).thenReturn(OperationResult.Success(versionedEntity))
      `when`(assessmentStrategy.create(any())).thenReturn(OperationResult.Success(versionedEntity))

      `when`(oasysAssociationsService.storeAssociation(any())).thenReturn(OperationResult.Success(Unit))

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)
      val response = (result as OasysCoordinatorService.CreateOperationResult.Success).data
      assertEquals(versionedEntity.id, response.sentencePlanId)

      verify(strategyFactory).getStrategies()
      verify(oasysAssociationsService, times(2)).storeAssociation(any())
    }

    @Test
    fun `should rollback on command execution failure`() {
      val planStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.PLAN }
      val assessmentStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }

      `when`(oasysAssociationsService.ensureNoExistingAssociation(anyString()))
        .thenReturn(OperationResult.Success(Unit))

      `when`(strategyFactory.getStrategies()).thenReturn(listOf(assessmentStrategy, planStrategy))

      `when`(oasysAssociationsService.storeAssociation(any()))
        .thenReturn(OperationResult.Success(Unit))

      `when`(assessmentStrategy.create(any())).thenReturn(OperationResult.Success(versionedEntity))
      `when`(planStrategy.create(any())).thenReturn(OperationResult.Failure<Nothing>("Execution failed"))

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Failure)
      assertEquals(
        "Failed to create entity for PLAN: Execution failed",
        (result as OasysCoordinatorService.CreateOperationResult.Failure).errorMessage,
      )

      verify(assessmentStrategy).delete(any())
    }

    @Test
    fun `should rollback on association storage failure`() {
      val strategy: EntityStrategy = mock { on { entityType } doReturn EntityType.PLAN }

      `when`(oasysAssociationsService.ensureNoExistingAssociation(anyString()))
        .thenReturn(OperationResult.Success(Unit))

      `when`(strategyFactory.getStrategies()).thenReturn(listOf(strategy))

      `when`(strategy.create(any())).thenReturn(OperationResult.Success(versionedEntity))

      `when`(oasysAssociationsService.storeAssociation(any())).thenReturn(OperationResult.Failure("Storage failed"))

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Failure)
      assertEquals(
        "Failed saving association for PLAN",
        (result as OasysCoordinatorService.CreateOperationResult.Failure).errorMessage,
      )

      verify(strategy).delete(any())
    }

    @Test
    fun `should return conflicting associations result if associations exist`() {
      `when`(oasysAssociationsService.ensureNoExistingAssociation(anyString()))
        .thenReturn(OperationResult.Failure("Conflicting associations"))

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.ConflictingAssociations)
      assertEquals(
        "Cannot create due to conflicting associations: Conflicting associations",
        (result as OasysCoordinatorService.CreateOperationResult.ConflictingAssociations).errorMessage,
      )

      verify(oasysAssociationsService).ensureNoExistingAssociation(oasysCreateRequest.oasysAssessmentPk)
    }
  }

  @Nested
  inner class Get {

    @Test
    fun `should return failure when no associations are found`() {
      `when`(oasysAssociationsService.findAssociations(anyString())).thenReturn(emptyList())

      val result = oasysCoordinatorService.get("CY/12ZX56")

      assertTrue(result is OasysCoordinatorService.GetOperationResult.NoAssociations)
      assertEquals(
        "No associations found for the provided OASys Assessment PK",
        (result as OasysCoordinatorService.GetOperationResult.NoAssociations).errorMessage,
      )
    }

    @Test
    fun `should return failure when strategy is not initialized`() {
      val association = OasysAssociation(
        id = 1L,
        entityType = null,
        entityUuid = UUID.randomUUID(),
        oasysAssessmentPk = "CY/12ZX56",
        regionPrisonCode = "111111",
      )
      `when`(oasysAssociationsService.findAssociations(anyString())).thenReturn(listOf(association))

      val result = oasysCoordinatorService.get("CY/12ZX56")

      assertTrue(result is OasysCoordinatorService.GetOperationResult.Failure)
      assertEquals(
        "Strategy not initialized for null",
        (result as OasysCoordinatorService.GetOperationResult.Failure).errorMessage,
      )
    }

    @Test
    fun `should return failure when command execution fails`() {
      val entityUuid = UUID.randomUUID()
      val association = OasysAssociation(
        id = 1L,
        entityType = EntityType.PLAN,
        entityUuid = entityUuid,
        oasysAssessmentPk = "CY/12ZX56",
        regionPrisonCode = "111111",
      )
      val strategy: EntityStrategy = mock()

      `when`(oasysAssociationsService.findAssociations(anyString())).thenReturn(listOf(association))
      `when`(strategyFactory.getStrategy(EntityType.PLAN)).thenReturn(strategy)

      `when`(strategy.fetch(any())).thenReturn(OperationResult.Failure<Nothing>("Execution failed"))

      val result = oasysCoordinatorService.get("CY/12ZX56")

      assertTrue(result is OasysCoordinatorService.GetOperationResult.Failure)
      assertEquals(
        "Failed to retrieve PLAN entity, Execution failed",
        (result as OasysCoordinatorService.GetOperationResult.Failure).errorMessage,
      )
    }

    @Test
    fun `should return success when fetch is successful`() {
      val entityUuid = UUID.randomUUID()
      val association = OasysAssociation(
        id = 1L,
        entityType = EntityType.PLAN,
        entityUuid = entityUuid,
        oasysAssessmentPk = "CY/12ZX56",
        regionPrisonCode = "111111",
      )
      val strategy: EntityStrategy = mock()
      val fetchResponse = VersionedEntity(entityUuid, 1, EntityType.PLAN)

      `when`(oasysAssociationsService.findAssociations(anyString())).thenReturn(listOf(association))
      `when`(strategyFactory.getStrategy(EntityType.PLAN)).thenReturn(strategy)

      `when`(strategy.fetch(any())).thenReturn(OperationResult.Success(fetchResponse))

      val result = oasysCoordinatorService.get("CY/12ZX56")

      assertTrue(result is OasysCoordinatorService.GetOperationResult.Success)
      val response = (result as OasysCoordinatorService.GetOperationResult.Success).data
      assertNotNull(response)
    }
  }
}

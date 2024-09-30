package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.CommandFactory
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.CreateCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.StrategyFactory
import java.util.UUID

class OasysCoordinatorServiceTest {
  private val commandFactory: CommandFactory = mock()
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
    oasysCoordinatorService = OasysCoordinatorService(commandFactory, strategyFactory, oasysAssociationsService)
  }

  @Nested
  inner class Create {

    @Test
    fun `should create entities and associations successfully`() {
      val command: CreateCommand = mock()
      val strategy: EntityStrategy = mock { on { entityType } doReturn EntityType.PLAN }

      `when`(oasysAssociationsService.ensureNoExistingAssociation(anyString()))
        .thenReturn(OperationResult.Success(Unit))

      `when`(strategyFactory.getStrategies()).thenReturn(listOf(strategy))

      `when`(commandFactory.createCommand(eq(strategy), any())).thenReturn(command)
      `when`(command.execute()).thenReturn(OperationResult.Success(versionedEntity))

      `when`(oasysAssociationsService.storeAssociation(any())).thenReturn(OperationResult.Success(Unit))

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)
      val response = (result as OasysCoordinatorService.CreateOperationResult.Success).data
      assertEquals(versionedEntity.id, response.sentencePlanId)

      verify(strategyFactory).getStrategies()
      verify(commandFactory).createCommand(eq(strategy), any<CreateData>())
      verify(oasysAssociationsService).ensureNoExistingAssociation(oasysCreateRequest.oasysAssessmentPk)
      verify(oasysAssociationsService, times(1)).storeAssociation(any())
    }

    @Test
    fun `should create entities and associations successfully for both PLAN and ASSESSMENT`() {
      val planStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.PLAN }
      val assessmentStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }
      val command: CreateCommand = mock()

      `when`(oasysAssociationsService.ensureNoExistingAssociation(anyString()))
        .thenReturn(OperationResult.Success(Unit))

      `when`(strategyFactory.getStrategies()).thenReturn(listOf(planStrategy, assessmentStrategy))

      `when`(commandFactory.createCommand(any(), any())).thenReturn(command)

      `when`(command.execute()).thenReturn(OperationResult.Success(versionedEntity))

      `when`(oasysAssociationsService.storeAssociation(any())).thenReturn(OperationResult.Success(Unit))

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)
      val response = (result as OasysCoordinatorService.CreateOperationResult.Success).data
      assertEquals(versionedEntity.id, response.sentencePlanId)

      verify(strategyFactory).getStrategies()
      verify(commandFactory).createCommand(eq(planStrategy), any())
      verify(commandFactory).createCommand(eq(assessmentStrategy), any())
      verify(oasysAssociationsService, times(2)).storeAssociation(any())
    }

    @Test
    fun `should rollback on command execution failure`() {
      val planStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.PLAN }
      val assessmentStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }
      val commandOne: CreateCommand = mock()
      val commandTwo: CreateCommand = mock()

      `when`(oasysAssociationsService.ensureNoExistingAssociation(anyString()))
        .thenReturn(OperationResult.Success(Unit))

      `when`(strategyFactory.getStrategies()).thenReturn(listOf(assessmentStrategy, planStrategy))

      `when`(oasysAssociationsService.storeAssociation(any()))
        .thenReturn(OperationResult.Success(Unit))

      `when`(commandOne.execute()).thenReturn(OperationResult.Success(versionedEntity))
      `when`(commandTwo.execute()).thenReturn(OperationResult.Failure<Nothing>("Execution failed"))

      `when`(commandFactory.createCommand(any(), any()))
        .thenReturn(commandOne)
        .thenReturn(commandTwo)

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Failure)
      assertEquals(
        "Failed to create entity for PLAN: Execution failed",
        (result as OasysCoordinatorService.CreateOperationResult.Failure).errorMessage,
      )

      verify(commandOne).rollback()
    }

    @Test
    fun `should rollback on association storage failure`() {
      val command: CreateCommand = mock()
      val strategy: EntityStrategy = mock { on { entityType } doReturn EntityType.PLAN }

      `when`(oasysAssociationsService.ensureNoExistingAssociation(anyString()))
        .thenReturn(OperationResult.Success(Unit))

      `when`(strategyFactory.getStrategies()).thenReturn(listOf(strategy))

      `when`(commandFactory.createCommand(eq(strategy), any())).thenReturn(command)
      `when`(command.execute()).thenReturn(OperationResult.Success(versionedEntity))

      `when`(oasysAssociationsService.storeAssociation(any())).thenReturn(OperationResult.Failure("Storage failed"))

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Failure)
      assertEquals(
        "Failed saving association for PLAN",
        (result as OasysCoordinatorService.CreateOperationResult.Failure).errorMessage,
      )

      verify(command).rollback()
      verify(commandFactory).createCommand(eq(strategy), any<CreateData>())
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
      verify(commandFactory, never()).createCommand(any(), any())
    }
  }
}

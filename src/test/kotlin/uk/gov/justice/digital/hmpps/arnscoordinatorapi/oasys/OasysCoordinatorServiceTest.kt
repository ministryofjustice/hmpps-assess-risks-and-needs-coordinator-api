package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.interceptor.TransactionAspectSupport
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SoftDeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.StrategyFactory
import java.time.LocalDateTime
import java.util.UUID

class OasysCoordinatorServiceTest {
  private val strategyFactory: StrategyFactory = mock()
  private val oasysAssociationsService: OasysAssociationsService = mock()

  private lateinit var oasysCoordinatorService: OasysCoordinatorService

  private val oasysCreateRequest = OasysCreateRequest(
    oasysAssessmentPk = "CY/12ZX56",
    regionPrisonCode = "111111",
    planType = PlanType.INITIAL,
    assessmentType = AssessmentType.SAN_SP,
    userDetails = OasysUserDetails(id = "userId", name = "John Doe"),
  )

  private val versionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.PLAN)

  @BeforeEach
  fun setup() {
    oasysCoordinatorService = OasysCoordinatorService(strategyFactory, oasysAssociationsService)
  }

  @Nested
  inner class Create {

    @BeforeEach
    fun createSetUp() {
      `when`(oasysAssociationsService.ensureNoExistingAssociation(anyString()))
        .thenReturn(OperationResult.Success(Unit))
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

    @Test
    fun `should create entities and associations successfully`() {
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }
      val sanStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }
      val spVersionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.AAP_PLAN)
      val sanVersionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.ASSESSMENT)

      `when`(strategyFactory.getStrategiesFor(AssessmentType.SAN_SP)).thenReturn(listOf(sanStrategy, spStrategy))

      `when`(spStrategy.create(any())).thenReturn(OperationResult.Success(spVersionedEntity))
      `when`(sanStrategy.create(any())).thenReturn(OperationResult.Success(sanVersionedEntity))

      `when`(oasysAssociationsService.storeAssociation(any())).thenReturn(OperationResult.Success(Unit))

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)
      val response = (result as OasysCoordinatorService.CreateOperationResult.Success).data
      assertEquals(spVersionedEntity.id, response.sentencePlanId)
      assertEquals(sanVersionedEntity.id, response.sanAssessmentId)

      verify(strategyFactory).getStrategiesFor(AssessmentType.SAN_SP)
      verify(oasysAssociationsService, times(2)).storeAssociation(any())
    }

    @Test
    fun `should create entities and associations successfully for both SAN and SP`() {
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }
      val sanStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }
      val spVersionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.AAP_PLAN)
      val sanVersionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.ASSESSMENT)

      `when`(strategyFactory.getStrategiesFor(AssessmentType.SAN_SP)).thenReturn(listOf(sanStrategy, spStrategy))

      `when`(spStrategy.create(any())).thenReturn(OperationResult.Success(spVersionedEntity))
      `when`(sanStrategy.create(any())).thenReturn(OperationResult.Success(sanVersionedEntity))

      `when`(oasysAssociationsService.storeAssociation(any())).thenReturn(OperationResult.Success(Unit))

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)
      val response = (result as OasysCoordinatorService.CreateOperationResult.Success).data
      assertEquals(spVersionedEntity.id, response.sentencePlanId)
      assertEquals(sanVersionedEntity.id, response.sanAssessmentId)

      verify(strategyFactory).getStrategiesFor(AssessmentType.SAN_SP)
      verify(oasysAssociationsService, times(2)).storeAssociation(any())
    }

    @Test
    fun `should rollback on command execution failure`() {
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }
      val sanStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }
      val sanVersionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.ASSESSMENT)

      `when`(strategyFactory.getStrategiesFor(AssessmentType.SAN_SP)).thenReturn(listOf(sanStrategy, spStrategy))

      `when`(oasysAssociationsService.storeAssociation(any()))
        .thenReturn(OperationResult.Success(Unit))

      `when`(sanStrategy.create(any())).thenReturn(OperationResult.Success(sanVersionedEntity))
      `when`(spStrategy.create(any())).thenReturn(OperationResult.Failure<Nothing>("Execution failed"))

      val transactionStatus: TransactionStatus = mock()
      val transactionAspect: MockedStatic<TransactionAspectSupport> = mockStatic(TransactionAspectSupport::class.java)
      transactionAspect.`when`<TransactionStatus> { TransactionAspectSupport.currentTransactionStatus() }.thenReturn(transactionStatus)

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Failure)
      assertEquals(
        "Failed to create AAP_PLAN: Execution failed",
        (result as OasysCoordinatorService.CreateOperationResult.Failure).errorMessage,
      )

      verify(transactionStatus).setRollbackOnly()
      transactionAspect.close()
    }

    @Test
    fun `should rollback on association storage failure`() {
      val sanStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }
      val sanVersionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.ASSESSMENT)
      val spVersionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.AAP_PLAN)

      `when`(strategyFactory.getStrategiesFor(AssessmentType.SAN_SP)).thenReturn(listOf(sanStrategy, spStrategy))

      `when`(sanStrategy.create(any())).thenReturn(OperationResult.Success(sanVersionedEntity))
      `when`(spStrategy.create(any())).thenReturn(OperationResult.Success(spVersionedEntity))

      `when`(oasysAssociationsService.storeAssociation(any())).thenReturn(OperationResult.Failure("Storage failed"))

      val transactionStatus: TransactionStatus = mock()
      val transactionAspect: MockedStatic<TransactionAspectSupport> = mockStatic(TransactionAspectSupport::class.java)
      transactionAspect.`when`<TransactionStatus> { TransactionAspectSupport.currentTransactionStatus() }.thenReturn(transactionStatus)

      val result = oasysCoordinatorService.create(oasysCreateRequest)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Failure)
      assertTrue(
        (result as OasysCoordinatorService.CreateOperationResult.Failure).errorMessage.contains("Failed to store"),
      )

      verify(transactionStatus).setRollbackOnly()
      transactionAspect.close()
    }

    @Test
    fun `should link existing SAN and create new SP when previousOasysSanPk is provided`() {
      val previousSanPk = "previous123"
      val existingSanUuid = UUID.randomUUID()
      val existingSanAssociation = OasysAssociation(
        oasysAssessmentPk = previousSanPk,
        entityType = EntityType.ASSESSMENT,
        entityUuid = existingSanUuid,
        baseVersion = 5,
      )
      val requestWithPreviousSan = OasysCreateRequest(
        oasysAssessmentPk = "new456",
        previousOasysSanPk = previousSanPk,
        regionPrisonCode = "111111",
        planType = PlanType.INITIAL,
        assessmentType = AssessmentType.SAN_SP,
        userDetails = OasysUserDetails(id = "userId", name = "John Doe"),
      )
      val sanStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }
      val spVersionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.AAP_PLAN)

      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>()))
        .thenReturn(emptyList())
      `when`(strategyFactory.getStrategiesFor(AssessmentType.SAN_SP)).thenReturn(listOf(sanStrategy, spStrategy))
      `when`(oasysAssociationsService.findAssociationsByPkAndType(eq(previousSanPk), any()))
        .thenReturn(listOf(existingSanAssociation))
      `when`(oasysAssociationsService.storeAssociation(any()))
        .thenReturn(OperationResult.Success(Unit))
      `when`(spStrategy.create(any())).thenReturn(OperationResult.Success(spVersionedEntity))

      val result = oasysCoordinatorService.create(requestWithPreviousSan)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)
      val response = (result as OasysCoordinatorService.CreateOperationResult.Success).data
      assertEquals(existingSanUuid, response.sanAssessmentId)
      assertEquals(spVersionedEntity.id, response.sentencePlanId)

      verify(oasysAssociationsService).findAssociationsByPkAndType(eq(previousSanPk), any())
      verify(sanStrategy, never()).create(any())
      verify(spStrategy).create(any())
      verify(oasysAssociationsService, times(2)).storeAssociation(any())
    }

    @Test
    fun `should create new SAN and link existing SP when previousOasysSpPk is provided`() {
      val previousSpPk = "previous123"
      val existingSpUuid = UUID.randomUUID()
      val existingSpAssociation = OasysAssociation(
        oasysAssessmentPk = previousSpPk,
        entityType = EntityType.AAP_PLAN,
        entityUuid = existingSpUuid,
        baseVersion = 3,
      )
      val requestWithPreviousSp = OasysCreateRequest(
        oasysAssessmentPk = "new456",
        previousOasysSpPk = previousSpPk,
        regionPrisonCode = "111111",
        planType = PlanType.INITIAL,
        assessmentType = AssessmentType.SAN_SP,
        userDetails = OasysUserDetails(id = "userId", name = "John Doe"),
      )
      val sanStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }
      val sanVersionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.ASSESSMENT)

      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>()))
        .thenReturn(emptyList())
      `when`(strategyFactory.getStrategiesFor(AssessmentType.SAN_SP)).thenReturn(listOf(sanStrategy, spStrategy))
      `when`(oasysAssociationsService.findAssociationsByPkAndType(eq(previousSpPk), any()))
        .thenReturn(listOf(existingSpAssociation))
      `when`(oasysAssociationsService.storeAssociation(any()))
        .thenReturn(OperationResult.Success(Unit))
      `when`(sanStrategy.create(any())).thenReturn(OperationResult.Success(sanVersionedEntity))

      val result = oasysCoordinatorService.create(requestWithPreviousSp)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)
      val response = (result as OasysCoordinatorService.CreateOperationResult.Success).data
      assertEquals(sanVersionedEntity.id, response.sanAssessmentId)
      assertEquals(existingSpUuid, response.sentencePlanId)

      verify(oasysAssociationsService).findAssociationsByPkAndType(eq(previousSpPk), any())
      verify(sanStrategy).create(any())
      verify(spStrategy, never()).create(any())
      verify(oasysAssociationsService, times(2)).storeAssociation(any())
    }

    @Test
    fun `should link both existing SAN and SP when both previous PKs are provided`() {
      val previousSanPk = "previousSan123"
      val previousSpPk = "previousSp456"
      val existingSanUuid = UUID.randomUUID()
      val existingSpUuid = UUID.randomUUID()
      val existingSanAssociation = OasysAssociation(
        oasysAssessmentPk = previousSanPk,
        entityType = EntityType.ASSESSMENT,
        entityUuid = existingSanUuid,
        baseVersion = 5,
      )
      val existingSpAssociation = OasysAssociation(
        oasysAssessmentPk = previousSpPk,
        entityType = EntityType.AAP_PLAN,
        entityUuid = existingSpUuid,
        baseVersion = 3,
      )
      val requestWithBothPrevious = OasysCreateRequest(
        oasysAssessmentPk = "new789",
        previousOasysSanPk = previousSanPk,
        previousOasysSpPk = previousSpPk,
        regionPrisonCode = "111111",
        planType = PlanType.INITIAL,
        assessmentType = AssessmentType.SAN_SP,
        userDetails = OasysUserDetails(id = "userId", name = "John Doe"),
      )
      val sanStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }

      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>()))
        .thenReturn(emptyList())
      `when`(strategyFactory.getStrategiesFor(AssessmentType.SAN_SP)).thenReturn(listOf(sanStrategy, spStrategy))
      `when`(oasysAssociationsService.findAssociationsByPkAndType(eq(previousSanPk), any()))
        .thenReturn(listOf(existingSanAssociation))
      `when`(oasysAssociationsService.findAssociationsByPkAndType(eq(previousSpPk), any()))
        .thenReturn(listOf(existingSpAssociation))
      `when`(oasysAssociationsService.storeAssociation(any()))
        .thenReturn(OperationResult.Success(Unit))

      val result = oasysCoordinatorService.create(requestWithBothPrevious)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)
      val response = (result as OasysCoordinatorService.CreateOperationResult.Success).data
      assertEquals(existingSanUuid, response.sanAssessmentId)
      assertEquals(existingSpUuid, response.sentencePlanId)

      verify(sanStrategy, never()).create(any())
      verify(spStrategy, never()).create(any())
      verify(oasysAssociationsService, times(2)).storeAssociation(any())
    }

    @Test
    fun `should return not found when previousOasysSanPk has no association`() {
      val previousSanPk = "nonexistent123"
      val requestWithPreviousSan = OasysCreateRequest(
        oasysAssessmentPk = "new456",
        previousOasysSanPk = previousSanPk,
        regionPrisonCode = "111111",
        planType = PlanType.INITIAL,
        assessmentType = AssessmentType.SAN_SP,
        userDetails = OasysUserDetails(id = "userId", name = "John Doe"),
      )
      val sanStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }
      val spVersionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.AAP_PLAN)

      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>()))
        .thenReturn(emptyList())
      `when`(strategyFactory.getStrategiesFor(AssessmentType.SAN_SP)).thenReturn(listOf(sanStrategy, spStrategy))
      `when`(oasysAssociationsService.findAssociationsByPkAndType(eq(previousSanPk), any()))
        .thenReturn(emptyList())
      `when`(spStrategy.create(any())).thenReturn(OperationResult.Success(spVersionedEntity))
      `when`(oasysAssociationsService.storeAssociation(any())).thenReturn(OperationResult.Success(Unit))

      val transactionStatus: TransactionStatus = mock()
      val transactionAspect: MockedStatic<TransactionAspectSupport> = mockStatic(TransactionAspectSupport::class.java)
      transactionAspect.`when`<TransactionStatus> { TransactionAspectSupport.currentTransactionStatus() }.thenReturn(transactionStatus)

      val result = oasysCoordinatorService.create(requestWithPreviousSan)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.NoAssociations)
      assertEquals(
        "No ASSESSMENT association found for PK $previousSanPk",
        (result as OasysCoordinatorService.CreateOperationResult.NoAssociations).errorMessage,
      )

      transactionAspect.close()
    }

    @Test
    fun `should return not found when previousOasysSpPk has no association`() {
      val previousSpPk = "nonexistent123"
      val requestWithPreviousSp = OasysCreateRequest(
        oasysAssessmentPk = "new456",
        previousOasysSpPk = previousSpPk,
        regionPrisonCode = "111111",
        planType = PlanType.INITIAL,
        assessmentType = AssessmentType.SP,
        userDetails = OasysUserDetails(id = "userId", name = "John Doe"),
      )
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }

      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>()))
        .thenReturn(emptyList())
      `when`(strategyFactory.getStrategiesFor(AssessmentType.SP)).thenReturn(listOf(spStrategy))
      `when`(oasysAssociationsService.findAssociationsByPkAndType(eq(previousSpPk), any()))
        .thenReturn(emptyList())

      val transactionStatus: TransactionStatus = mock()
      val transactionAspect: MockedStatic<TransactionAspectSupport> = mockStatic(TransactionAspectSupport::class.java)
      transactionAspect.`when`<TransactionStatus> { TransactionAspectSupport.currentTransactionStatus() }.thenReturn(transactionStatus)

      val result = oasysCoordinatorService.create(requestWithPreviousSp)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.NoAssociations)
      assertEquals(
        "No AAP_PLAN association found for PK $previousSpPk",
        (result as OasysCoordinatorService.CreateOperationResult.NoAssociations).errorMessage,
      )

      transactionAspect.close()
    }

    @Test
    fun `should link existing SP when assessmentType is SP and previousOasysSpPk is provided`() {
      val previousSpPk = "previous123"
      val existingSpUuid = UUID.randomUUID()
      val existingSpAssociation = OasysAssociation(
        oasysAssessmentPk = previousSpPk,
        entityType = EntityType.AAP_PLAN,
        entityUuid = existingSpUuid,
        baseVersion = 3,
      )
      val requestSpOnly = OasysCreateRequest(
        oasysAssessmentPk = "new456",
        previousOasysSpPk = previousSpPk,
        regionPrisonCode = "111111",
        planType = PlanType.INITIAL,
        assessmentType = AssessmentType.SP,
        userDetails = OasysUserDetails(id = "userId", name = "John Doe"),
      )
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }

      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>()))
        .thenReturn(emptyList())
      `when`(strategyFactory.getStrategiesFor(AssessmentType.SP)).thenReturn(listOf(spStrategy))
      `when`(oasysAssociationsService.findAssociationsByPkAndType(eq(previousSpPk), any()))
        .thenReturn(listOf(existingSpAssociation))
      `when`(oasysAssociationsService.storeAssociation(any()))
        .thenReturn(OperationResult.Success(Unit))

      val result = oasysCoordinatorService.create(requestSpOnly)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)
      val response = (result as OasysCoordinatorService.CreateOperationResult.Success).data
      assertEquals(existingSpUuid, response.sentencePlanId)
      assertEquals(UUID(0, 0), response.sanAssessmentId)

      verify(spStrategy, never()).create(any())
      verify(oasysAssociationsService, times(1)).storeAssociation(any())
    }

    @Test
    fun `should call reset when linking existing SP with newPeriodOfSupervision true`() {
      val previousSpPk = "previous123"
      val existingSpUuid = UUID.randomUUID()
      val existingSpAssociation = OasysAssociation(
        oasysAssessmentPk = previousSpPk,
        entityType = EntityType.AAP_PLAN,
        entityUuid = existingSpUuid,
        baseVersion = 3,
      )
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }
      val resetVersionedEntity = VersionedEntity(existingSpUuid, 4, EntityType.AAP_PLAN)

      val requestWithReset = OasysCreateRequest(
        oasysAssessmentPk = "new456",
        previousOasysSpPk = previousSpPk,
        regionPrisonCode = "111111",
        planType = PlanType.INITIAL,
        assessmentType = AssessmentType.SP,
        userDetails = OasysUserDetails(id = "userId", name = "John Doe"),
        newPeriodOfSupervision = "Y",
      )

      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>()))
        .thenReturn(emptyList())
      `when`(strategyFactory.getStrategiesFor(AssessmentType.SP)).thenReturn(listOf(spStrategy))
      `when`(oasysAssociationsService.findAssociationsByPkAndType(eq(previousSpPk), any()))
        .thenReturn(listOf(existingSpAssociation))
      `when`(oasysAssociationsService.storeAssociation(any()))
        .thenReturn(OperationResult.Success(Unit))
      `when`(spStrategy.reset(any(), eq(existingSpUuid))).thenReturn(OperationResult.Success(resetVersionedEntity))

      val result = oasysCoordinatorService.create(requestWithReset)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)
      val response = (result as OasysCoordinatorService.CreateOperationResult.Success).data
      assertEquals(existingSpUuid, response.sentencePlanId)

      verify(spStrategy).reset(any(), eq(existingSpUuid))
    }

    @Test
    fun `should not call reset when linking existing SP with newPeriodOfSupervision false`() {
      val previousSpPk = "previous123"
      val existingSpUuid = UUID.randomUUID()
      val existingSpAssociation = OasysAssociation(
        oasysAssessmentPk = previousSpPk,
        entityType = EntityType.AAP_PLAN,
        entityUuid = existingSpUuid,
        baseVersion = 3,
      )
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }

      val requestWithoutReset = OasysCreateRequest(
        oasysAssessmentPk = "new456",
        previousOasysSpPk = previousSpPk,
        regionPrisonCode = "111111",
        planType = PlanType.INITIAL,
        assessmentType = AssessmentType.SP,
        userDetails = OasysUserDetails(id = "userId", name = "John Doe"),
        newPeriodOfSupervision = "N",
      )

      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>()))
        .thenReturn(emptyList())
      `when`(strategyFactory.getStrategiesFor(AssessmentType.SP)).thenReturn(listOf(spStrategy))
      `when`(oasysAssociationsService.findAssociationsByPkAndType(eq(previousSpPk), any()))
        .thenReturn(listOf(existingSpAssociation))
      `when`(oasysAssociationsService.storeAssociation(any()))
        .thenReturn(OperationResult.Success(Unit))

      val result = oasysCoordinatorService.create(requestWithoutReset)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Success)

      verify(spStrategy, never()).create(any())
      verify(spStrategy, never()).reset(any(), any())
    }

    @Test
    fun `should return failure when reset fails during new period of supervision`() {
      val previousSpPk = "previous123"
      val existingSpUuid = UUID.randomUUID()
      val existingSpAssociation = OasysAssociation(
        oasysAssessmentPk = previousSpPk,
        entityType = EntityType.AAP_PLAN,
        entityUuid = existingSpUuid,
        baseVersion = 3,
      )
      val spStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.AAP_PLAN }

      val requestWithReset = OasysCreateRequest(
        oasysAssessmentPk = "new456",
        previousOasysSpPk = previousSpPk,
        regionPrisonCode = "111111",
        planType = PlanType.INITIAL,
        assessmentType = AssessmentType.SP,
        userDetails = OasysUserDetails(id = "userId", name = "John Doe"),
        newPeriodOfSupervision = "Y",
      )

      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>()))
        .thenReturn(emptyList())
      `when`(strategyFactory.getStrategiesFor(AssessmentType.SP)).thenReturn(listOf(spStrategy))
      `when`(oasysAssociationsService.findAssociationsByPkAndType(eq(previousSpPk), any()))
        .thenReturn(listOf(existingSpAssociation))
      `when`(oasysAssociationsService.storeAssociation(any()))
        .thenReturn(OperationResult.Success(Unit))
      `when`(spStrategy.reset(any(), eq(existingSpUuid))).thenReturn(OperationResult.Failure("Reset failed"))

      val transactionStatus: TransactionStatus = mock()
      val transactionAspect: MockedStatic<TransactionAspectSupport> = mockStatic(TransactionAspectSupport::class.java)
      transactionAspect.`when`<TransactionStatus> { TransactionAspectSupport.currentTransactionStatus() }.thenReturn(transactionStatus)

      val result = oasysCoordinatorService.create(requestWithReset)

      assertTrue(result is OasysCoordinatorService.CreateOperationResult.Failure)
      assertEquals(
        "Failed to reset AAP_PLAN: Reset failed",
        (result as OasysCoordinatorService.CreateOperationResult.Failure).errorMessage,
      )

      verify(transactionStatus).setRollbackOnly()
      transactionAspect.close()
    }
  }

  @Nested
  inner class Get {

    @Test
    fun `should return failure when no associations are found`() {
      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>())).thenReturn(emptyList())

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
      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>())).thenReturn(listOf(association))

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

      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>())).thenReturn(listOf(association))
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

      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>())).thenReturn(listOf(association))
      `when`(strategyFactory.getStrategy(EntityType.PLAN)).thenReturn(strategy)

      `when`(strategy.fetch(any())).thenReturn(OperationResult.Success(fetchResponse))

      val result = oasysCoordinatorService.get("CY/12ZX56")

      assertTrue(result is OasysCoordinatorService.GetOperationResult.Success)
      val response = (result as OasysCoordinatorService.GetOperationResult.Success).data
      assertNotNull(response)
    }
  }

  @Nested
  inner class SoftDelete {
    val request = OasysGenericRequest(OasysUserDetails())
    val oasysAssessmentPk = "CY/12ZX56"

    @Test
    fun `should return failure when no associations are found`() {
      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>())).thenReturn(emptyList())

      val result = oasysCoordinatorService.softDelete(request, oasysAssessmentPk)

      assertTrue(result is OasysCoordinatorService.SoftDeleteOperationResult.NoAssociations)
      assertEquals(
        "No associations found for the provided OASys Assessment PK",
        (result as OasysCoordinatorService.SoftDeleteOperationResult.NoAssociations).errorMessage,
      )
    }

    @Test
    fun `should return failure when strategy is not initialized`() {
      val association = OasysAssociation(
        id = 1L,
        entityType = null,
        entityUuid = UUID.randomUUID(),
        oasysAssessmentPk = oasysAssessmentPk,
        regionPrisonCode = "111111",
      )
      `when`(oasysAssociationsService.findAssociationsByPk(anyString(), anyOrNull<Boolean>())).thenReturn(listOf(association))

      val result = oasysCoordinatorService.softDelete(request, oasysAssessmentPk)

      assertTrue(result is OasysCoordinatorService.SoftDeleteOperationResult.Failure)
      assertEquals(
        "Strategy not initialized for null",
        (result as OasysCoordinatorService.SoftDeleteOperationResult.Failure).errorMessage,
      )
    }

    @Test
    fun `should return failure when command execution fails`() {
      val association = OasysAssociation(
        id = 1L,
        entityType = EntityType.PLAN,
        oasysAssessmentPk = oasysAssessmentPk,
        regionPrisonCode = "111111",
        baseVersion = 1,
      )
      val strategy: EntityStrategy = mock()

      `when`(oasysAssociationsService.findAssociationsByPk(eq(oasysAssessmentPk), anyOrNull<Boolean>())).thenReturn(listOf(association))
      `when`(oasysAssociationsService.findAllIncludingDeleted(association.entityUuid)).thenReturn(listOf(association))
      `when`(strategyFactory.getStrategy(EntityType.PLAN)).thenReturn(strategy)

      val expectedSoftDeleteData = SoftDeleteData(request.userDetails.intoUserDetails(), 1)

      `when`(strategy.softDelete(expectedSoftDeleteData, association.entityUuid)).thenReturn(OperationResult.Failure<Nothing>("Execution failed"))

      val result = oasysCoordinatorService.softDelete(request, oasysAssessmentPk)

      assertTrue(result is OasysCoordinatorService.SoftDeleteOperationResult.Failure)
      assertEquals(
        "Failed to soft-delete association for $oasysAssessmentPk, Execution failed",
        (result as OasysCoordinatorService.SoftDeleteOperationResult.Failure).errorMessage,
      )
    }

    @Test
    fun `should return success when soft-delete is successful`() {
      val entityUuid = UUID.randomUUID()
      val associations = listOf(
        OasysAssociation(
          id = 1L,
          createdAt = LocalDateTime.now().minusDays(2),
          entityUuid = entityUuid,
          entityType = EntityType.PLAN,
          oasysAssessmentPk = "older-oasys-pk",
          regionPrisonCode = "111111",
          baseVersion = 0L,
        ),
        OasysAssociation(
          id = 1L,
          createdAt = LocalDateTime.now().minusDays(1),
          entityUuid = entityUuid,
          entityType = EntityType.PLAN,
          oasysAssessmentPk = oasysAssessmentPk,
          regionPrisonCode = "111111",
          baseVersion = 1L,
        ),
        OasysAssociation(
          id = 2L,
          createdAt = LocalDateTime.now(),
          entityUuid = entityUuid,
          entityType = EntityType.PLAN,
          oasysAssessmentPk = "newer-oasys-pk",
          regionPrisonCode = "111111",
          baseVersion = 2L,
          deleted = true,
        ),
      )

      val strategy: EntityStrategy = mock()
      val softDeleteResponse = VersionedEntity(entityUuid, 3, EntityType.PLAN)
      `when`(strategy.softDelete(argThat { it: SoftDeleteData -> it.versionFrom == 1L && it.versionTo == 2L }, eq(entityUuid))).thenReturn(OperationResult.Success(softDeleteResponse))

      `when`(oasysAssociationsService.findAssociationsByPk(eq(oasysAssessmentPk), anyOrNull<Boolean>())).thenReturn(associations.filter { it.oasysAssessmentPk == oasysAssessmentPk })
      `when`(oasysAssociationsService.findAllIncludingDeleted(entityUuid)).thenReturn(associations)
      `when`(oasysAssociationsService.storeAssociation(argThat { it: OasysAssociation -> it.deleted && it.baseVersion == 1L })).then { i -> OperationResult.Success(i.getArgument<OasysAssociation>(0)) }

      `when`(strategyFactory.getStrategy(EntityType.PLAN)).thenReturn(strategy)

      val result = oasysCoordinatorService.softDelete(request, oasysAssessmentPk)

      assertTrue(result is OasysCoordinatorService.SoftDeleteOperationResult.Success)
      val response = (result as OasysCoordinatorService.SoftDeleteOperationResult.Success).data

      assertNotNull(response)
      assertEquals(entityUuid, response.sentencePlanId)
      assertEquals(3, response.sentencePlanVersion)
    }
  }

  @Nested
  inner class GetVersionsByEntityId {
    private val entityUuid = UUID.randomUUID()
    private val oasysAssessmentPk = "test-pk"

    private val planAssociation = OasysAssociation(
      entityType = EntityType.PLAN,
      entityUuid = UUID.randomUUID(),
      oasysAssessmentPk = oasysAssessmentPk,
    )

    private val assessmentAssociation = OasysAssociation(
      entityType = EntityType.ASSESSMENT,
      entityUuid = UUID.randomUUID(),
      oasysAssessmentPk = oasysAssessmentPk,
    )

    private val planStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.PLAN }
    private val assessmentStrategy: EntityStrategy = mock { on { entityType } doReturn EntityType.ASSESSMENT }

    @Test
    fun `should return only PLAN versions when authType is HMPPS_AUTH`() {
      val authType = "HMPPS_AUTH"

      `when`(oasysAssociationsService.findOasysPkByEntityId(entityUuid))
        .thenReturn(oasysAssessmentPk)

      `when`(oasysAssociationsService.findAssociationsByPkAndType(oasysAssessmentPk, listOf(EntityType.PLAN)))
        .thenReturn(listOf(planAssociation))

      `when`(oasysAssociationsService.findAssociationsByPk(eq(oasysAssessmentPk), anyOrNull())).thenReturn(emptyList())

      `when`(strategyFactory.getStrategy(EntityType.PLAN)).thenReturn(planStrategy)
      `when`(planStrategy.fetchVersions(planAssociation.entityUuid)).thenReturn(
        OperationResult.Success(emptyList()),
      )

      val result = oasysCoordinatorService.getVersionsByEntityId(entityUuid, authType)

      assertTrue(result is OasysCoordinatorService.GetOperationResult.Success)
      verify(oasysAssociationsService).findOasysPkByEntityId(entityUuid)
      verify(oasysAssociationsService).findAssociationsByPkAndType(oasysAssessmentPk, listOf(EntityType.PLAN))

      verify(planStrategy).fetchVersions(planAssociation.entityUuid)
      verify(assessmentStrategy, times(0)).fetchVersions(any())
    }

    @Test
    fun `should return both PLAN and ASSESSMENT versions when authType is not HMPPS_AUTH`() {
      val authType = "OASYS" // or null

      `when`(oasysAssociationsService.findOasysPkByEntityId(entityUuid))
        .thenReturn(oasysAssessmentPk)

      `when`(oasysAssociationsService.findAssociationsByPk(eq(oasysAssessmentPk), anyOrNull()))
        .thenReturn(listOf(planAssociation, assessmentAssociation))

      `when`(oasysAssociationsService.findAssociationsByPkAndType(oasysAssessmentPk, listOf(EntityType.PLAN))).thenReturn(emptyList())

      `when`(strategyFactory.getStrategy(EntityType.PLAN)).thenReturn(planStrategy)
      `when`(strategyFactory.getStrategy(EntityType.ASSESSMENT)).thenReturn(assessmentStrategy)
      `when`(planStrategy.fetchVersions(planAssociation.entityUuid)).thenReturn(
        OperationResult.Success(emptyList()),
      )
      `when`(assessmentStrategy.fetchVersions(assessmentAssociation.entityUuid)).thenReturn(
        OperationResult.Success(emptyList()),
      )

      val result = oasysCoordinatorService.getVersionsByEntityId(entityUuid, authType)

      assertTrue(result is OasysCoordinatorService.GetOperationResult.Success)
      verify(oasysAssociationsService).findOasysPkByEntityId(entityUuid)
      verify(oasysAssociationsService).findAssociationsByPk(eq(oasysAssessmentPk), anyOrNull())
      verify(planStrategy).fetchVersions(planAssociation.entityUuid)
      verify(assessmentStrategy).fetchVersions(assessmentAssociation.entityUuid)
    }

    @Test
    fun `should return NoAssociations if initial PK lookup fails`() {
      `when`(oasysAssociationsService.findOasysPkByEntityId(entityUuid))
        .thenReturn(null)

      val result = oasysCoordinatorService.getVersionsByEntityId(entityUuid, "HMPPS_AUTH")

      assertTrue(result is OasysCoordinatorService.GetOperationResult.NoAssociations)
      assertEquals("No associations found for the provided entityUuid", (result as OasysCoordinatorService.GetOperationResult.NoAssociations).errorMessage)
    }
  }
}

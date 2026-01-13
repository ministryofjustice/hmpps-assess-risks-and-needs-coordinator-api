package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.AAPApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.SingleValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SoftDeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.GetPlanResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanState
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.service.OasysVersionService
import java.time.LocalDateTime
import java.util.UUID

class AAPPlanStrategyTest {
  private val aapApi: AAPApi = mock()
  private val oasysVersionService: OasysVersionService = mock()
  private val clock: Clock = mock()
  private lateinit var planStrategy: AAPPlanStrategy

  private val now = LocalDateTime.parse("2026-01-09T12:00:00")

  @BeforeEach
  fun setup() {
    planStrategy = AAPPlanStrategy(
      aapApi = aapApi,
      oasysVersionService = oasysVersionService,
      clock = clock,
    )

    whenever(clock.now()).thenReturn(now)
  }

  @Nested
  inner class Create {

    @Test
    fun `should return success when create plan is successful`() {
      val createData = CreateData(
        plan = CreatePlanData(PlanType.INITIAL, UserDetails("id", "name")),
      )
      val versionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.PLAN)

      whenever(aapApi.createAssessment(any())).thenReturn(
        AAPApi.ApiOperationResult.Success(versionedEntity),
      )

      whenever(oasysVersionService.createVersionFor(OasysEvent.CREATED, versionedEntity.id))
        .thenReturn(
          OasysVersionEntity(
            createdBy = OasysEvent.CREATED,
            entityUuid = versionedEntity.id,
            version = versionedEntity.version,
          ),
        )

      val result = planStrategy.create(createData)

      assertTrue(result is OperationResult.Success)
      assertEquals(versionedEntity, (result as OperationResult.Success).data)
      verify(aapApi).createAssessment(any())
    }

    @Test
    fun `should return failure when create plan fails`() {
      val createData = CreateData(
        plan = CreatePlanData(PlanType.INITIAL, UserDetails("id", "name")),
      )

      whenever(aapApi.createAssessment(any())).thenReturn(
        AAPApi.ApiOperationResult.Failure("Error occurred"),
      )

      val result = planStrategy.create(createData)

      assertTrue(result is OperationResult.Failure)
      assertEquals("Error occurred", (result as OperationResult.Failure).errorMessage)
      verify(aapApi).createAssessment(any())
    }
  }

  @Nested
  inner class Fetch {

    @Test
    fun `should return success when fetch plan is successful`() {
      val entityUuid = UUID.randomUUID()
      val queryResult = AssessmentVersionQueryResult(
        assessmentUuid = entityUuid,
        aggregateUuid = UUID.randomUUID(),
        assessmentType = "PLAN",
        formVersion = "v1.0",
        createdAt = LocalDateTime.parse("2026-01-09T12:00:00"),
        updatedAt = LocalDateTime.parse("2026-01-09T12:30:00"),
        answers = emptyMap(),
        properties = mapOf(
          "PLAN_STATE" to SingleValue("INCOMPLETE"),
          "PLAN_TYPE" to SingleValue("INITIAL"),
        ),
        collaborators = emptySet(),
        identifiers = emptyMap(),
      )

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Success(queryResult),
      )
      whenever(oasysVersionService.getLatestVersionNumberForEntityUuid(entityUuid)).thenReturn(1)

      val result = planStrategy.fetch(entityUuid)

      assertTrue(result is OperationResult.Success)
      assertEquals(
        GetPlanResponse(
          sentencePlanId = queryResult.assessmentUuid,
          sentencePlanVersion = 1,
          planComplete = PlanState.INCOMPLETE,
          planType = PlanType.INITIAL,
          lastUpdatedTimestampSP = queryResult.updatedAt,
        ),
        (result as OperationResult.Success).data,
      )
      verify(aapApi).fetchAssessment(entityUuid, now)
      verify(oasysVersionService).getLatestVersionNumberForEntityUuid(entityUuid)
    }

    @Test
    fun `should return failure when unable to parse PLAN_STATE`() {
      val entityUuid = UUID.randomUUID()
      val queryResult = AssessmentVersionQueryResult(
        assessmentUuid = entityUuid,
        aggregateUuid = UUID.randomUUID(),
        assessmentType = "PLAN",
        formVersion = "v1.0",
        createdAt = LocalDateTime.parse("2026-01-09T12:00:00"),
        updatedAt = LocalDateTime.parse("2026-01-09T12:30:00"),
        answers = emptyMap(),
        properties = mapOf(
          "PLAN_STATE" to SingleValue("FOO_VALUE"),
          "PLAN_TYPE" to SingleValue("INITIAL"),
        ),
        collaborators = emptySet(),
        identifiers = emptyMap(),
      )

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Success(queryResult),
      )
      whenever(oasysVersionService.getLatestVersionNumberForEntityUuid(entityUuid)).thenReturn(1)

      val result = planStrategy.fetch(entityUuid)

      assertTrue(result is OperationResult.Failure)
      assertEquals("Unable to parse version for entity $entityUuid", (result as OperationResult.Failure).errorMessage)
      verify(aapApi).fetchAssessment(entityUuid, now)
      verify(oasysVersionService).getLatestVersionNumberForEntityUuid(entityUuid)
    }

    @Test
    fun `should return failure when there is no value for PLAN_STATE`() {
      val entityUuid = UUID.randomUUID()
      val queryResult = AssessmentVersionQueryResult(
        assessmentUuid = entityUuid,
        aggregateUuid = UUID.randomUUID(),
        assessmentType = "PLAN",
        formVersion = "v1.0",
        createdAt = LocalDateTime.parse("2026-01-09T12:00:00"),
        updatedAt = LocalDateTime.parse("2026-01-09T12:30:00"),
        answers = emptyMap(),
        properties = mapOf(
          "PLAN_TYPE" to SingleValue("INITIAL"),
        ),
        collaborators = emptySet(),
        identifiers = emptyMap(),
      )

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Success(queryResult),
      )
      whenever(oasysVersionService.getLatestVersionNumberForEntityUuid(entityUuid)).thenReturn(1)

      val result = planStrategy.fetch(entityUuid)

      assertTrue(result is OperationResult.Failure)
      assertEquals("No value for PLAN_STATE for entity $entityUuid", (result as OperationResult.Failure).errorMessage)
      verify(aapApi).fetchAssessment(entityUuid, now)
      verify(oasysVersionService).getLatestVersionNumberForEntityUuid(entityUuid)
    }

    @Test
    fun `should return failure when unable to parse  PLAN_TYPE`() {
      val entityUuid = UUID.randomUUID()
      val queryResult = AssessmentVersionQueryResult(
        assessmentUuid = entityUuid,
        aggregateUuid = UUID.randomUUID(),
        assessmentType = "PLAN",
        formVersion = "v1.0",
        createdAt = LocalDateTime.parse("2026-01-09T12:00:00"),
        updatedAt = LocalDateTime.parse("2026-01-09T12:30:00"),
        answers = emptyMap(),
        properties = mapOf(
          "PLAN_STATE" to SingleValue("INCOMPLETE"),
          "PLAN_TYPE" to SingleValue("FOO_VALUE"),
        ),
        collaborators = emptySet(),
        identifiers = emptyMap(),
      )

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Success(queryResult),
      )
      whenever(oasysVersionService.getLatestVersionNumberForEntityUuid(entityUuid)).thenReturn(1)

      val result = planStrategy.fetch(entityUuid)

      assertTrue(result is OperationResult.Failure)
      assertEquals("Unable to parse version for entity $entityUuid", (result as OperationResult.Failure).errorMessage)
      verify(aapApi).fetchAssessment(entityUuid, now)
      verify(oasysVersionService).getLatestVersionNumberForEntityUuid(entityUuid)
    }

    @Test
    fun `should return failure when there is no value for PLAN_TYPE`() {
      val entityUuid = UUID.randomUUID()
      val queryResult = AssessmentVersionQueryResult(
        assessmentUuid = entityUuid,
        aggregateUuid = UUID.randomUUID(),
        assessmentType = "PLAN",
        formVersion = "v1.0",
        createdAt = LocalDateTime.parse("2026-01-09T12:00:00"),
        updatedAt = LocalDateTime.parse("2026-01-09T12:30:00"),
        answers = emptyMap(),
        properties = mapOf(
          "PLAN_STATE" to SingleValue("INCOMPLETE"),
        ),
        collaborators = emptySet(),
        identifiers = emptyMap(),
      )

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Success(queryResult),
      )
      whenever(oasysVersionService.getLatestVersionNumberForEntityUuid(entityUuid)).thenReturn(1)

      val result = planStrategy.fetch(entityUuid)

      assertTrue(result is OperationResult.Failure)
      assertEquals("No value for PLAN_TYPE for entity $entityUuid", (result as OperationResult.Failure).errorMessage)
      verify(aapApi).fetchAssessment(entityUuid, now)
      verify(oasysVersionService).getLatestVersionNumberForEntityUuid(entityUuid)
    }

    @Test
    fun `should return failure when fetch plan fails`() {
      val entityUuid = UUID.randomUUID()

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Failure("Fetch error occurred"),
      )

      val result = planStrategy.fetch(entityUuid)

      assertTrue(result is OperationResult.Failure)
      assertEquals("Fetch error occurred", (result as OperationResult.Failure).errorMessage)
      verify(aapApi).fetchAssessment(entityUuid, now)
    }
  }

  @Nested
  inner class Rollback {
    val request = OasysRollbackRequest(
      sanVersionNumber = null,
      sentencePlanVersionNumber = 1,
      userDetails = OasysUserDetails("id", "name"),
    )
    val versionedEntity = OasysVersionEntity(
      createdBy = OasysEvent.AWAITING_COUNTERSIGN,
      entityUuid = UUID.randomUUID(),
      version = 1,
    )

    @Test
    fun `should return success when rollback assessment is successful`() {
      whenever(
        oasysVersionService.updateVersion(
          OasysEvent.ROLLED_BACK,
          versionedEntity.entityUuid,
          versionedEntity.version,
        ),
      ).thenReturn(
        versionedEntity,
      )

      val result = planStrategy.rollback(request, versionedEntity.entityUuid)

      assertTrue(result is OperationResult.Success)
      (result as OperationResult.Success).data.let {
        assertEquals(it.version, versionedEntity.version)
        assertEquals(it.id, versionedEntity.entityUuid)
      }
      verify(oasysVersionService).updateVersion(
        OasysEvent.ROLLED_BACK,
        versionedEntity.entityUuid,
        versionedEntity.version,
      )
    }

    @Test
    fun `should return failure when unable to find the version to roll back`() {
      whenever(
        oasysVersionService.updateVersion(
          OasysEvent.ROLLED_BACK,
          versionedEntity.entityUuid,
          versionedEntity.version,
        ),
      )
        .thenReturn(null)

      val result = planStrategy.rollback(request, versionedEntity.entityUuid)

      assertTrue(result is OperationResult.Failure)
      assertEquals(
        "Unable to find version '${request.sentencePlanVersionNumber}' for entity ${versionedEntity.entityUuid}",
        (result as OperationResult.Failure).errorMessage,
      )
      verify(oasysVersionService).updateVersion(
        OasysEvent.ROLLED_BACK,
        versionedEntity.entityUuid,
        versionedEntity.version,
      )
    }

    @Test
    fun `should return failure when updating the plan fails`() {
      whenever(
        oasysVersionService.updateVersion(
          OasysEvent.ROLLED_BACK,
          versionedEntity.entityUuid,
          versionedEntity.version,
        ),
      )
        .thenThrow(RuntimeException("Error occurred"))

      val result = planStrategy.rollback(request, versionedEntity.entityUuid)

      assertTrue(result is OperationResult.Failure)
      assertEquals(
        "Failed to update version for entity ${versionedEntity.entityUuid}",
        (result as OperationResult.Failure).errorMessage,
      )
      verify(oasysVersionService).updateVersion(
        OasysEvent.ROLLED_BACK,
        versionedEntity.entityUuid,
        versionedEntity.version,
      )
    }
  }

  @Nested
  inner class Lock {
    val versionedEntity = OasysVersionEntity(
      createdBy = OasysEvent.AWAITING_COUNTERSIGN,
      entityUuid = UUID.randomUUID(),
      version = 3,
    )

    @Test
    fun `should return success when the plan is locked successfully`() {
      whenever(
        oasysVersionService.createVersionFor(
          OasysEvent.LOCKED,
          versionedEntity.entityUuid,
        ),
      ).thenReturn(
        versionedEntity,
      )

      val result = planStrategy.lock(
        LockData(
          userDetails = UserDetails("id", "name"),
        ),
        versionedEntity.entityUuid,
      )

      verify(oasysVersionService).createVersionFor(
        OasysEvent.LOCKED,
        versionedEntity.entityUuid,
      )
      assertTrue(result is OperationResult.Success)
      (result as OperationResult.Success).data
        .let {
          assertEquals(it.version, versionedEntity.version)
          assertEquals(it.id, versionedEntity.entityUuid)
        }
    }

    @Test
    fun `should return failure when lock plan fails`() {
      whenever(
        oasysVersionService.createVersionFor(
          OasysEvent.LOCKED,
          versionedEntity.entityUuid,
        ),
      ).thenThrow(RuntimeException("Error occurred"))

      val result = planStrategy.lock(
        LockData(
          userDetails = UserDetails("id", "name"),
        ),
        versionedEntity.entityUuid,
      )

      verify(oasysVersionService).createVersionFor(
        OasysEvent.LOCKED,
        versionedEntity.entityUuid,
      )
      assertTrue(result is OperationResult.Failure)
      assertEquals(
        "Failed to lock plan for entity ${versionedEntity.entityUuid}",
        (result as OperationResult.Failure).errorMessage,
      )
    }
  }

  @Nested
  inner class Sign {
    val versionedEntity = OasysVersionEntity(
      createdBy = OasysEvent.AWAITING_COUNTERSIGN,
      entityUuid = UUID.randomUUID(),
      version = 3,
    )

    @Test
    fun `should return success when the plan is self signed successfully`() {
      whenever(
        oasysVersionService.createVersionFor(
          OasysEvent.SELF_SIGNED,
          versionedEntity.entityUuid,
        ),
      ).thenReturn(
        versionedEntity,
      )

      val result = planStrategy.sign(
        signData = SignData(
          signType = SignType.SELF,
          userDetails = UserDetails("id", "name"),
        ),
        versionedEntity.entityUuid,
      )

      verify(oasysVersionService).createVersionFor(
        OasysEvent.SELF_SIGNED,
        versionedEntity.entityUuid,
      )
      assertTrue(result is OperationResult.Success)
      (result as OperationResult.Success).data
        .let {
          assertEquals(it.version, versionedEntity.version)
          assertEquals(it.id, versionedEntity.entityUuid)
        }
    }

    @Test
    fun `should return success when the plan is sent to countersign successfully`() {
      whenever(
        oasysVersionService.createVersionFor(
          OasysEvent.AWAITING_COUNTERSIGN,
          versionedEntity.entityUuid,
        ),
      ).thenReturn(
        versionedEntity,
      )

      val result = planStrategy.sign(
        signData = SignData(
          signType = SignType.COUNTERSIGN,
          userDetails = UserDetails("id", "name"),
        ),
        versionedEntity.entityUuid,
      )

      verify(oasysVersionService).createVersionFor(
        OasysEvent.AWAITING_COUNTERSIGN,
        versionedEntity.entityUuid,
      )
      assertTrue(result is OperationResult.Success)
      (result as OperationResult.Success).data
        .let {
          assertEquals(it.version, versionedEntity.version)
          assertEquals(it.id, versionedEntity.entityUuid)
        }
    }

    @Test
    fun `should return failure when countersign plan fails`() {
      whenever(
        oasysVersionService.createVersionFor(
          OasysEvent.AWAITING_COUNTERSIGN,
          versionedEntity.entityUuid,
        ),
      ).thenThrow(RuntimeException("Error occurred"))

      val result = planStrategy.sign(
        signData = SignData(
          signType = SignType.COUNTERSIGN,
          userDetails = UserDetails("id", "name"),
        ),
        versionedEntity.entityUuid,
      )

      verify(oasysVersionService).createVersionFor(
        OasysEvent.AWAITING_COUNTERSIGN,
        versionedEntity.entityUuid,
      )
      assertTrue(result is OperationResult.Failure)
      assertEquals(
        "Failed to sign the plan for entity ${versionedEntity.entityUuid}",
        (result as OperationResult.Failure).errorMessage,
      )
    }
  }

  @Nested
  inner class CounterSign {
    val versionedEntity = OasysVersionEntity(
      createdBy = OasysEvent.AWAITING_COUNTERSIGN,
      entityUuid = UUID.randomUUID(),
      version = 3,
    )

    @ParameterizedTest(name = "{0} maps to {1}")
    @CsvSource(
      "COUNTERSIGNED, COUNTERSIGNED",
      "AWAITING_DOUBLE_COUNTERSIGN, AWAITING_DOUBLE_COUNTERSIGN",
      "DOUBLE_COUNTERSIGNED, DOUBLE_COUNTERSIGNED",
      "REJECTED, REJECTED",
    )
    fun `should return success when countersign plan is successful`(oasysEvent: OasysEvent, outcome: CounterSignOutcome) {
      whenever(
        oasysVersionService.updateVersion(
          oasysEvent,
          versionedEntity.entityUuid,
          versionedEntity.version,
        ),
      ).thenReturn(
        versionedEntity,
      )

      val result = planStrategy.counterSign(
        versionedEntity.entityUuid,
        OasysCounterSignRequest(
          sanVersionNumber = 3,
          sentencePlanVersionNumber = 3,
          outcome = outcome,
          userDetails = OasysUserDetails("1", "OASys User"),
        ),
      )

      verify(oasysVersionService).updateVersion(
        oasysEvent,
        versionedEntity.entityUuid,
        versionedEntity.version,
      )
      assertTrue(result is OperationResult.Success)
      (result as OperationResult.Success).data
        .let {
          assertEquals(it.version, versionedEntity.version)
          assertEquals(it.id, versionedEntity.entityUuid)
        }
    }

    @Test
    fun `should return failure when countersign plan fails`() {
      whenever(
        oasysVersionService.updateVersion(
          OasysEvent.COUNTERSIGNED,
          versionedEntity.entityUuid,
          versionedEntity.version,
        ),
      ).thenThrow(RuntimeException("Error occurred"))

      val result = planStrategy.counterSign(
        versionedEntity.entityUuid,
        OasysCounterSignRequest(
          sanVersionNumber = 1,
          sentencePlanVersionNumber = versionedEntity.version,
          outcome = CounterSignOutcome.COUNTERSIGNED,
          userDetails = OasysUserDetails("1", "OASys User"),
        ),
      )

      verify(oasysVersionService).updateVersion(
        OasysEvent.COUNTERSIGNED,
        versionedEntity.entityUuid,
        versionedEntity.version,
      )
      assertTrue(result is OperationResult.Failure)
      assertEquals(
        "Unable to countersign version for entity ${versionedEntity.entityUuid}",
        (result as OperationResult.Failure).errorMessage,
      )
    }
  }

  @Nested
  inner class SoftDelete {
    val softDeleteData = SoftDeleteData(
      UserDetails("id", "name", UserType.OASYS),
      versionFrom = 1,
      versionTo = 2,
    )
    val versionedEntity = OasysVersionEntity(
      createdBy = OasysEvent.LOCKED,
      entityUuid = UUID.randomUUID(),
      version = 2,
    )

    @Test
    fun `should return success when soft-delete is successful`() {
      whenever(oasysVersionService.softDeleteVersions(versionedEntity.entityUuid, 1, 2)).thenReturn(
        versionedEntity,
      )

      val result = planStrategy.softDelete(softDeleteData, versionedEntity.entityUuid)

      assertTrue(result is OperationResult.Success)
      (result as OperationResult.Success).data?.let {
        assertEquals(it.id, versionedEntity.entityUuid)
        assertEquals(it.version, versionedEntity.version)
      }
      verify(oasysVersionService).softDeleteVersions(versionedEntity.entityUuid, 1, 2)
    }

    @Test
    fun `should return failure when soft-delete fails`() {
      whenever(oasysVersionService.softDeleteVersions(versionedEntity.entityUuid, 1, 2))
        .thenThrow(RuntimeException("Error occurred"))

      val result = planStrategy.softDelete(softDeleteData, versionedEntity.entityUuid)

      assertTrue(result is OperationResult.Failure)
      assertEquals(
        result,
        OperationResult.Failure<VersionedEntity?>("Something went wrong while deleting versions for entity ${versionedEntity.entityUuid}"),
      )
      verify(oasysVersionService).softDeleteVersions(versionedEntity.entityUuid, 1, 2)
    }
  }
}

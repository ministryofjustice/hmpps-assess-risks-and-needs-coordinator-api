package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.AAPApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.DeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SoftDeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UndeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import java.util.UUID

class AAPPlanStrategyTest {
  private val aapApi: AAPApi = mock()
  private lateinit var aapPlanStrategy: AAPPlanStrategy

  @BeforeEach
  fun setup() {
    aapPlanStrategy = AAPPlanStrategy(aapApi)
  }

  @Nested
  inner class EntityType {

    @Test
    fun `should return AAP_PLAN entity type`() {
      assertEquals(uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType.AAP_PLAN, aapPlanStrategy.entityType)
    }
  }

  @Nested
  inner class Create {

    @Test
    fun `should return success when create assessment is successful`() {
      val createData = CreateData(
        plan = CreatePlanData(PlanType.PLAN_ONLY, UserDetails("id", "name")),
      )
      val versionedEntity = VersionedEntity(UUID.randomUUID(), 0, uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType.AAP_PLAN)

      `when`(aapApi.createAssessment(any())).thenReturn(
        AAPApi.ApiOperationResult.Success(versionedEntity),
      )

      val result = aapPlanStrategy.create(createData)

      assertTrue(result is OperationResult.Success)
      assertEquals(versionedEntity, (result as OperationResult.Success).data)
      verify(aapApi).createAssessment(any())
    }

    @Test
    fun `should return failure when create assessment fails`() {
      val createData = CreateData(
        plan = CreatePlanData(PlanType.PLAN_ONLY, UserDetails("id", "name")),
      )

      `when`(aapApi.createAssessment(any())).thenReturn(
        AAPApi.ApiOperationResult.Failure("Error occurred"),
      )

      val result = aapPlanStrategy.create(createData)

      assertTrue(result is OperationResult.Failure)
      assertEquals("Error occurred", (result as OperationResult.Failure).errorMessage)
      verify(aapApi).createAssessment(any())
    }
  }
}

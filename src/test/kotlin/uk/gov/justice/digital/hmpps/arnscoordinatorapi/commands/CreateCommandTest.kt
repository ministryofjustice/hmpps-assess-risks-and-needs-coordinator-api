package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.DeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class CreateCommandTest {
  private val entityStrategy: EntityStrategy = mock()
  private lateinit var createCommand: CreateCommand

  private val createData = CreateData(
    plan = CreatePlanData(PlanType.INITIAL, UserDetails("id", "name")),
    assessment = null,
  )

  private val versionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.PLAN)

  @BeforeEach
  fun setup() {
    createCommand = CreateCommand(entityStrategy, createData)
  }

  @Nested
  inner class Execute {

    @Test
    fun `should return success and set created entity when create is successful`() {
      `when`(entityStrategy.create(any())).thenReturn(OperationResult.Success(versionedEntity))

      val result = createCommand.execute()

      assertTrue(result is OperationResult.Success)
      assertEquals(versionedEntity, (result as OperationResult.Success).data)

      assertNotNull(createCommand.createdEntity)
      assertEquals(versionedEntity, createCommand.createdEntity)

      verify(entityStrategy).create(any())
    }

    @Test
    fun `should return failure when create fails`() {
      `when`(entityStrategy.create(any())).thenReturn(OperationResult.Failure("Creation failed"))

      val result = createCommand.execute()

      assertTrue(result is OperationResult.Failure)
      assertEquals("Creation failed", (result as OperationResult.Failure).errorMessage)

      verify(entityStrategy).create(any())
    }
  }

  @Nested
  inner class Rollback {
    val uuid: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
      createCommand.createdEntity = mock()
      `when`(createCommand.createdEntity.id).thenReturn(uuid)
    }

    @Test
    fun `should return success when rollback is successful`() {
      `when`(entityStrategy.delete(any(), any())).thenReturn(OperationResult.Success(Unit))

      val result = createCommand.rollback()

      assertTrue(result is OperationResult.Success)
      assertEquals(Unit, (result as OperationResult.Success).data)

      verify(entityStrategy).delete(DeleteData.from(createData), uuid)
    }

    @Test
    fun `should return failure when rollback fails`() {
      `when`(entityStrategy.delete(any(), any())).thenReturn(OperationResult.Failure("Test"))

      val result = createCommand.rollback()

      assertTrue(result is OperationResult.Failure)
      assertEquals("Test", (result as OperationResult.Failure).errorMessage)

      verify(entityStrategy).delete(DeleteData.from(createData), uuid)
    }
  }
}

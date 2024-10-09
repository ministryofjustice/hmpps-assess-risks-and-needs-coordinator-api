package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class RollbackCommandTest {
  private val entityStrategy: EntityStrategy = mock()
  private lateinit var rollbackCommand: RollbackCommand

  private val request = OasysRollbackRequest(
    sanVersionNumber = 1,
    sentencePlanVersionNumber = null,
    userDetails = OasysUserDetails("id", "name"),
  )

  private val versionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.ASSESSMENT)

  @BeforeEach
  fun setup() {
    rollbackCommand = RollbackCommand(entityStrategy, versionedEntity.id, request)
  }

  @Nested
  inner class Execute {

    @Test
    fun `should return success when rollback is successful`() {
      `when`(entityStrategy.rollback(request, versionedEntity.id)).thenReturn(OperationResult.Success(versionedEntity))

      val result = rollbackCommand.execute()

      assertTrue(result is OperationResult.Success)
      assertEquals(versionedEntity, (result as OperationResult.Success).data)

      verify(entityStrategy).rollback(request, versionedEntity.id)
    }

    @Test
    fun `should return failure when create fails`() {
      `when`(entityStrategy.rollback(request, versionedEntity.id)).thenReturn(OperationResult.Failure("Creation failed"))

      val result = rollbackCommand.execute()

      assertTrue(result is OperationResult.Failure)
      assertEquals("Creation failed", (result as OperationResult.Failure).errorMessage)

      verify(entityStrategy).rollback(request, versionedEntity.id)
    }
  }
}

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
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SoftDeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class SoftDeleteCommandTest {
  private val entityStrategy: EntityStrategy = mock()
  private lateinit var softDeleteCommand: SoftDeleteCommand

  private val softDeleteData = SoftDeleteData(
    userDetails = UserDetails("id", "name"),
    versionFrom = 1L,
    versionTo = 2L,
  )

  private val versionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.ASSESSMENT)

  @BeforeEach
  fun setup() {
    softDeleteCommand = SoftDeleteCommand(entityStrategy, versionedEntity.id, softDeleteData)
  }

  @Nested
  inner class Execute {

    @Test
    fun `should return success when soft-delete is successful`() {
      `when`(entityStrategy.softDelete(softDeleteData, versionedEntity.id)).thenReturn(OperationResult.Success(versionedEntity))

      val result = softDeleteCommand.execute()

      assertTrue(result is OperationResult.Success)
      assertEquals(versionedEntity, (result as OperationResult.Success).data)

      verify(entityStrategy).softDelete(softDeleteData, versionedEntity.id)
    }

    @Test
    fun `should return failure when soft-delete fails`() {
      `when`(entityStrategy.softDelete(softDeleteData, versionedEntity.id)).thenReturn(OperationResult.Failure("Soft-delete failed"))

      val result = softDeleteCommand.execute()

      assertTrue(result is OperationResult.Failure)
      assertEquals("Soft-delete failed", (result as OperationResult.Failure).errorMessage)

      verify(entityStrategy).softDelete(softDeleteData, versionedEntity.id)
    }
  }
}

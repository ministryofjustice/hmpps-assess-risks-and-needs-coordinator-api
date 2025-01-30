package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class LockCommand(
  private val strategy: EntityStrategy,
  private val entityUuid: UUID,
  private val lockData: LockData,
) : Command {

  lateinit var createdEntity: VersionedEntity

  override fun execute(): OperationResult<VersionedEntity> = when (val result = strategy.lock(lockData, entityUuid)) {
    is OperationResult.Success -> {
      result.also { createdEntity = it.data }
    }
    is OperationResult.Failure -> {
      result
    }
  }

  fun rollback(): OperationResult<Unit> = OperationResult.Failure("Rollback has not been implemented for lock")
}

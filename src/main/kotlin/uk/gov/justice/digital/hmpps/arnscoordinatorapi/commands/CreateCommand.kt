package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy

class CreateCommand(
  private val strategy: EntityStrategy,
  private val createData: CreateData,
) : Command {

  lateinit var createdEntity: VersionedEntity

  override fun execute(): OperationResult<VersionedEntity> {
    return when (val result = strategy.create(createData)) {
      is OperationResult.Success -> {
        result.also { createdEntity = it.data }
      }
      else -> result
    }
  }

  fun setCreatedEntity(createdEntity: VersionedEntity): CreateCommand {
    this.createdEntity = createdEntity
    return this
  }

  fun rollback(): OperationResult<Unit> {
    strategy.delete(createdEntity.id)
    return OperationResult.Failure("Rollback has not been implemented")
  }
}

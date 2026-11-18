package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.DeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy

class CreateCommand(
  private val strategy: EntityStrategy,
  private val createData: CreateData,
) : Command {

  lateinit var createdEntity: VersionedEntity

  override fun execute(): OperationResult<VersionedEntity> = when (val result = strategy.create(createData)) {
    is OperationResult.Success -> {
      result.also { createdEntity = it.data }
    }
    else -> result
  }

  fun rollback() = strategy.delete(DeleteData.from(createData), createdEntity.id)
}

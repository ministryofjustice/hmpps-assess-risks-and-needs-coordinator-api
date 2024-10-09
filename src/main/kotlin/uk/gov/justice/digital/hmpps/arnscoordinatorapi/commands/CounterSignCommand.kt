package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class CounterSignCommand(
  private val strategy: EntityStrategy,
  private val entityUuid: UUID,
  private val data: OasysCounterSignRequest,
) : Command {

  lateinit var versionedEntity: VersionedEntity

  override fun execute(): OperationResult<VersionedEntity> {
    return when (val result = strategy.counterSign(entityUuid, data)) {
      is OperationResult.Success -> {
        result.also { versionedEntity = it.data }
      }
      else -> result
    }
  }

  fun rollback(): OperationResult<Unit> {
    strategy.delete(versionedEntity.id)
    return OperationResult.Failure("Rollback has not been implemented")
  }
}

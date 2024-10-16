package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy

class CloneCommand(
  private val strategy: EntityStrategy,
  private val createData: CreateData,
) : Command {

  override fun execute(): OperationResult<VersionedEntity> {
    return strategy.clone(createData)
  }

  fun rollback(): OperationResult<Unit> {
    return OperationResult.Failure("Rollback has not been implemented")
  }
}

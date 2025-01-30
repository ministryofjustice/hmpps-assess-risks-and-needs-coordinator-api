package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class CloneCommand(
  private val strategy: EntityStrategy,
  private val createData: CreateData,
  private val entityUuid: UUID,
) : Command {

  override fun execute(): OperationResult<VersionedEntity> = strategy.clone(createData, entityUuid)

  fun rollback(): OperationResult<Unit> = OperationResult.Failure("Rollback has not been implemented")
}

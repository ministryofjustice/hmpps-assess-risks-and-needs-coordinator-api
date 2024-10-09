package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class RollbackCommand(
  private val strategy: EntityStrategy,
  private val entityUuid: UUID,
  private val request: OasysRollbackRequest,
) : Command {
  override fun execute(): OperationResult<VersionedEntity> {
    return strategy.rollback(request, entityUuid)
  }

  fun rollback(): OperationResult<Unit> {
    return OperationResult.Failure("Rollback has not been implemented for rollback")
  }
}

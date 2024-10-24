package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SoftDeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class SoftDeleteCommand(
  private val strategy: EntityStrategy,
  private val entityUuid: UUID,
  private val softDeleteData: SoftDeleteData,
) : Command {
  override fun execute(): OperationResult<VersionedEntity> = strategy.softDelete(softDeleteData, entityUuid)
}

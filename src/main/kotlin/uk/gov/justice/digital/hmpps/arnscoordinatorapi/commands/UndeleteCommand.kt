package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UndeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class UndeleteCommand(
  private val strategy: EntityStrategy,
  private val entityUuid: UUID,
  private val undeleteData: UndeleteData,
) : Command {
  override fun execute(): OperationResult<VersionedEntity> = strategy.undelete(undeleteData, entityUuid)
}

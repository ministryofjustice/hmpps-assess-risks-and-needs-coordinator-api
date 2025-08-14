package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetailsList
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class FetchVersionsCommand(
  private val strategy: EntityStrategy,
  private val entityUuid: UUID,
) : Command {

  override fun execute(): OperationResult<VersionDetailsList> = strategy.fetchVersions(entityUuid)
}


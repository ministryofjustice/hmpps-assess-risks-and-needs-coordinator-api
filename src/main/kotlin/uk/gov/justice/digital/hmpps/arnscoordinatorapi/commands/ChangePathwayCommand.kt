package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.PathwayChange
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.SanAssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class ChangePathwayCommand(
  private val strategy: EntityStrategy,
  private val entityUuid: UUID,
  private val newPathway: SanAssessmentType,
  private val userDetails: UserDetails,
) : RollbackableCommand {

  private var pathwayChange: PathwayChange? = null

  override fun execute(): OperationResult<PathwayChange?> = strategy.changePathway(entityUuid, newPathway, userDetails)
    .also { if (it is OperationResult.Success) pathwayChange = it.data }

  override fun rollback(): OperationResult<Unit> = pathwayChange
    ?.let { strategy.undoPathwayChange(entityUuid, it.clearedFrom, userDetails) }
    ?: OperationResult.Success(Unit)
}

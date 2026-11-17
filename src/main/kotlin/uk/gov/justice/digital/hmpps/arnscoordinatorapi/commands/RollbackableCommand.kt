package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult

interface RollbackableCommand : Command {
  fun rollback(): OperationResult<*>
}

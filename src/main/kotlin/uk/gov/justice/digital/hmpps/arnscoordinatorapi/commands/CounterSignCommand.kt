package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class CounterSignCommand(
  private val strategy: EntityStrategy,
  private val entityUuid: UUID,
  private val data: OasysCounterSignRequest,
) : Command {
  override fun execute() = strategy.counterSign(entityUuid, data)
}

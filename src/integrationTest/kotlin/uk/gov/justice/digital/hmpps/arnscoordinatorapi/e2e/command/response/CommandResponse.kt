package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request.Command

data class CommandResponse(
  val request: Command,
  val result: CommandResult,
)

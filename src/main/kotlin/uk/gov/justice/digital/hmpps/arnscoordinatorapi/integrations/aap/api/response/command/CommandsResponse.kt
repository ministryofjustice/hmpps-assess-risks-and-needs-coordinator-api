package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.Command

data class CommandsResponse(
  val commands: List<CommandResponse>,
)

data class CommandResponse(
  val request: Command,
  val result: CommandResult,
)

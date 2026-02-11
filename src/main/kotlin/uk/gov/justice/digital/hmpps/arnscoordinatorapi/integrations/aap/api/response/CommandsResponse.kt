package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response

data class CommandsResponse(
  val commands: List<CommandResponse>,
)

data class CommandResponse(
  val request: Map<String, Any?>,
  val result: CommandResult,
)

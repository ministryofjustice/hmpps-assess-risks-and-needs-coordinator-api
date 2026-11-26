package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.Command

data class CommandsResponse(
  val commands: List<CommandResponse>,
) {
  inline fun <reified T : CommandResult> extractNthInstance(index: Int): T = commands.map { it.result }
    .filterIsInstance<T>().getOrNull(index)
    ?: error("No response found at index $index for ${T::class.simpleName}")

  inline fun <reified T : CommandResult> extractSingle(): T = commands.map { it.result }
    .filterIsInstance<T>()
    .singleOrNull()
    ?: error("Expected exactly one ${T::class.simpleName}")
}

data class CommandResponse(
  val request: Command,
  val result: CommandResult,
)

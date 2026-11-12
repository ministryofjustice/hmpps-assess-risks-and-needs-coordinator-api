package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request

data class CommandsRequest(
  val commands: List<Command>,
) {
  companion object {
    fun of(vararg commands: Command) = CommandsRequest(commands.toList())
  }
}

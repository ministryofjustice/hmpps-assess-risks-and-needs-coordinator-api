package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command

data class CommandsRequest(
  val commands: List<Command>,
) {
  companion object {
    fun of(vararg commands: Command) = CommandsRequest(commands.toList())
  }
}

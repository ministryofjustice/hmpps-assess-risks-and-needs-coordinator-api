package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request

data class CommandsRequest(
  val commands: List<Any>,
) {
  companion object {
    fun of(vararg commands: Any) = CommandsRequest(commands.toList())
  }
}

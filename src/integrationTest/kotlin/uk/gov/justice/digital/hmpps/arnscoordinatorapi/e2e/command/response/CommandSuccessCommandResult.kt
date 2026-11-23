package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.response

data class CommandSuccessCommandResult(
  override val message: String = "Done",
) : CommandResult {
  override val success: Boolean = true
}

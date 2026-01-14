package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response

data class CommandSuccessResult(
  override val message: String = "Done",
  override val success: Boolean = true,
) : CommandResult

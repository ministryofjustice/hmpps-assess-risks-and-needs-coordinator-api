package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command

import java.util.UUID

data class CreateAssessmentCommandResult(
  val assessmentUuid: UUID,
  override val message: String = "",
  override val success: Boolean = true,
) : CommandResult

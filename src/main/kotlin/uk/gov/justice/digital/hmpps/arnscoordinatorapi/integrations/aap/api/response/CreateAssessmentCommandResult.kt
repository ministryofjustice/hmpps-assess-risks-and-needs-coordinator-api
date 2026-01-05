package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response

data class CreateAssessmentCommandResult(
  val type: String,
  val assessmentUuid: String,
)

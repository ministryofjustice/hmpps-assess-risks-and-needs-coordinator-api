package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request

data class CreateAssessmentCommand(
  val type: String = "CreateAssessmentCommand",
  val assessmentType: String,
  val formVersion: String,
  val properties: Map<String, PropertyValue>? = null,
  val user: AAPUser,
)

data class PropertyValue(
  val type: String,
  val value: String,
)

data class AAPUser(
  val id: String,
  val name: String,
)

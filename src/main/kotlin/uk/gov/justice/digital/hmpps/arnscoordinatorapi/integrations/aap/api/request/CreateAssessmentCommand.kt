package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request

data class CreateAssessmentCommand(
  val assessmentType: String,
  val formVersion: String,
  val properties: Map<String, PropertyValue>? = null,
  val identifiers: Map<IdentifierType, String>? = null,
  val user: AAPUser,
) {
  val type: String = "CreateAssessmentCommand"
}

data class PropertyValue(
  val type: String,
  val value: String,
)

data class AAPUser(
  val id: String,
  val name: String,
)

enum class IdentifierType {
  CRN,
  NOMIS_ID,
}

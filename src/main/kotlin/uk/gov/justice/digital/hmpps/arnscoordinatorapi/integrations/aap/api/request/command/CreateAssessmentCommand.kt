package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser

data class CreateAssessmentCommand(
  val assessmentType: String,
  val formVersion: String,
  val properties: Map<String, PropertyValue>? = null,
  val identifiers: Map<IdentifierType, String>? = null,
  override val user: AAPUser,
) : Command

data class PropertyValue(
  val type: String,
  val value: String,
)

enum class IdentifierType {
  CRN,
  NOMIS_ID,
}

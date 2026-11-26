package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook.Hook

data class CreateAssessmentCommand(
  val assessmentType: String,
  val formVersion: String,
  val properties: Map<String, PropertyValue>? = null,
  val identifiers: Map<IdentifierType, String>? = null,
  val flags: List<String> = emptyList(),
  override val user: AAPUser,
  override val hooks: List<Hook>? = null,
) : Command

data class PropertyValue(
  val type: String,
  val value: String,
)

enum class IdentifierType {
  CRN,
  NOMIS_ID,
}

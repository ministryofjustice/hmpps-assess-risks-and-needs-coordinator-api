package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.PropertyValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.Timeline
import java.util.UUID

data class UpdateAssessmentAnswersCommand(
  val user: AAPUser,
  val assessmentUuid: UUID,
  val added: Map<String, PropertyValue>,
  val removed: List<String>,
  val timeline: Timeline? = null,
  val hooks: List<String>? = null,
  val autosaved: Boolean = false,
) : Command

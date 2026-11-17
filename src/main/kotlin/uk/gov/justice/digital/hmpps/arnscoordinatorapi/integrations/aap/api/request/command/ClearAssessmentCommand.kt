package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook.Hook
import java.util.UUID

data class ClearAssessmentCommand(
  val assessmentUuid: UUID,
  override val user: AAPUser,
  val properties: List<String> = emptyList(),
  val collections: List<String> = emptyList(),
  val timeline: Timeline,
  override val hooks: List<Hook>? = null,
) : Command

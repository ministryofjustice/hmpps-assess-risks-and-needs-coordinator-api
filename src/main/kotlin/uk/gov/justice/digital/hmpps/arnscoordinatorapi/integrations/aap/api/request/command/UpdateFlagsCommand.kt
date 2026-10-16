package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook.Hook
import java.util.UUID

data class UpdateFlagsCommand(
  val assessmentUuid: UUID,
  override val user: AAPUser,
  val flags: List<String>,
  val timeline: Timeline? = null,
  override val hooks: List<Hook>? = null,
) : Command

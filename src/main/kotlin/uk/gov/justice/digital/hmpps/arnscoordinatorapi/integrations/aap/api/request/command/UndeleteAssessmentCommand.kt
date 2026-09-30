package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import java.time.LocalDateTime
import java.util.UUID

data class UndeleteAssessmentCommand(
  val assessmentUuid: UUID,
  override val user: AAPUser,
  val pointInTime: LocalDateTime,
) : Command

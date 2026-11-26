package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import java.util.UUID

data class CreateCollectionCommand(
  val name: String,
  val assessmentUuid: UUID,
  val user: AAPUser,
  val identifiers: Map<String, String>,
  val flags: List<String>,
) : Command

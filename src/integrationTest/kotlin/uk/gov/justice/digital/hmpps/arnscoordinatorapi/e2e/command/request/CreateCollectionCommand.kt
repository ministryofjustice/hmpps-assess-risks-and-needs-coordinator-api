package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import java.util.UUID

data class CreateCollectionCommand(
  val name: String,
  val assessmentUuid: UUID,
  val user: UserDetails,
  val identifiers: Map<String, String>,
  val flags: List<String>,
) : Command

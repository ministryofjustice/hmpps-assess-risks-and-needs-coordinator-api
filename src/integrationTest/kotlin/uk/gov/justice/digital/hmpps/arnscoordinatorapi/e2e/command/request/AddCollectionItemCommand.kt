package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.Value
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import java.util.UUID

data class AddCollectionItemCommand(
  val name: String,
  val assessmentUuid: UUID,
  val collectionUuid: UUID,
  val user: UserDetails,
  val identifiers: Map<String, String>,
  val flags: List<String>,
  val properties: Map<String, Value>,
  val answers: Map<String, Value>,
) : Command

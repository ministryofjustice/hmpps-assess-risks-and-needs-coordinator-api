package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.Timeline
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import java.util.UUID

data class CreateCollectionItemResult(
  val commands: List<AddCollectionItemCommandResponse>,
)

data class AddCollectionItemCommandResponse(
  val request: AddCollectionItemRequest,
  val result: AddCollectionItemCommandResult,
)

data class AddCollectionItemRequest(
  val type: String,
  val formVersion: String? = null,
  val timeline: Timeline? = null,
  val user: UserDetails,
)

data class AddCollectionItemCommandResult(
  val type: String,
  val collectionItemUuid: UUID,
  val message: String,
  val success: Boolean,
)

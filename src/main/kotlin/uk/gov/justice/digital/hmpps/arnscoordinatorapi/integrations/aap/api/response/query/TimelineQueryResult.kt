package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import java.time.LocalDateTime
import java.util.UUID

data class TimelineItem(
  val uuid: UUID,
  val timestamp: LocalDateTime,
  val user: AAPUser,
  val assessment: UUID,
  val event: String,
  var data: Map<String, Any> = mapOf(),
  var customType: String? = null,
  var customData: Map<String, Any>? = null,
)

data class PageInfo(
  val pageNumber: Int,
  val totalPages: Int,
)

data class TimelineQueryResult(
  val timeline: List<TimelineItem>,
  val pageInfo: PageInfo,
) : QueryResult

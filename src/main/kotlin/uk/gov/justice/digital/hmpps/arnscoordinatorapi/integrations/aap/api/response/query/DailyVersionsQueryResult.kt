package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query

import java.time.LocalDateTime
import java.util.UUID

data class DailyVersionsQueryResult(
  val versions: List<DailyVersionDetails>,
) : QueryResult

data class DailyVersionDetails(
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val lastTimelineItemUuid: UUID,
)

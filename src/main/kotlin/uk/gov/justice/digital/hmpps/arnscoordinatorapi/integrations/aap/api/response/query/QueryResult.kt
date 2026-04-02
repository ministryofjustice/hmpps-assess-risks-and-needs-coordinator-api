package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = AssessmentVersionQueryResult::class, name = "AssessmentVersionQueryResult"),
  JsonSubTypes.Type(value = DailyVersionsQueryResult::class, name = "DailyVersionsQueryResult"),
  JsonSubTypes.Type(value = TimelineQueryResult::class, name = "TimelineQueryResult"),
)
sealed interface QueryResult

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AssessmentIdentifier
import java.time.LocalDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = AssessmentVersionQuery::class, name = "AssessmentVersionQuery"),
  JsonSubTypes.Type(value = DailyVersionsQuery::class, name = "DailyVersionsQuery"),
  JsonSubTypes.Type(value = TimelineQuery::class, name = "TimelineQuery"),
)
sealed interface Query {
  val user: AAPUser
  val assessmentIdentifier: AssessmentIdentifier
  val timestamp: LocalDateTime?
}

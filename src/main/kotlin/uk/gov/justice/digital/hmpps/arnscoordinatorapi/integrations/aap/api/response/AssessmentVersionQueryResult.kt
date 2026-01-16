package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import java.time.LocalDateTime
import java.util.UUID

sealed interface Value
data class SingleValue(val value: String) : Value
data class MultiValue(val values: List<String>) : Value

typealias CollaboratorsView = Set<AAPUser>
typealias AnswersView = Map<String, Value>
typealias PropertiesView = Map<String, Value>

enum class IdentifierType {
  CRN,
}

data class AssessmentVersionQueryResult(
  val assessmentUuid: UUID,
  val aggregateUuid: UUID,
  val assessmentType: String,
  val formVersion: String,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val answers: AnswersView,
  val properties: PropertiesView,
  val collaborators: CollaboratorsView,
  val identifiers: Map<IdentifierType, String>,
)

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import java.time.LocalDateTime
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = SingleValue::class, name = "Single"),
  JsonSubTypes.Type(value = MultiValue::class, name = "Multi"),
)
sealed interface Value
data class SingleValue(val value: String) : Value
data class MultiValue(val values: List<String>) : Value

typealias CollaboratorsView = Set<AAPUser>
typealias AnswersView = Map<String, Value>
typealias PropertiesView = Map<String, Value>
typealias CollectionsView = List<Collection>

data class Collection(
  val uuid: UUID,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val name: String,
  val items: List<CollectionItem>,
)

data class CollectionItem(
  val uuid: UUID,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val answers: AnswersView,
  val properties: PropertiesView,
  val collections: CollectionsView,
)

enum class IdentifierType {
  CRN,
  NOMIS_ID,
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
  val collections: CollectionsView,
  val collaborators: CollaboratorsView,
  val identifiers: Map<IdentifierType, String>,
)

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.serialize.AssessmentDataDeserializer
import java.time.LocalDateTime
import java.util.UUID

typealias AssessmentData = Map<String, *>

data class AssessmentResponse(
  val metaData: AssessmentMetadata,
  @JsonDeserialize(using = AssessmentDataDeserializer::class)
  val assessment: AssessmentData,
  @JsonDeserialize(using = AssessmentDataDeserializer::class)
  val oasysEquivalent: AssessmentData,
)

data class AssessmentMetadata(
  val uuid: UUID,
  val createdAt: LocalDateTime,
  val versionUuid: UUID,
  val versionNumber: Long,
  val versionCreatedAt: LocalDateTime,
  val versionUpdatedAt: LocalDateTime,
  val formVersion: String?,
)

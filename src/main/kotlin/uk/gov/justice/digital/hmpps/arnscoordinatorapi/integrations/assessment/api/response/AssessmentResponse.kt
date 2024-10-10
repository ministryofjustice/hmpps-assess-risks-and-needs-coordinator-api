package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response

import java.time.LocalDateTime
import java.util.UUID

data class AssessmentResponse(
  val metaData: AssessmentMetadata,
  val assessment: Map<String, *>,
  val oasysEquivalent: Map<String, *>
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

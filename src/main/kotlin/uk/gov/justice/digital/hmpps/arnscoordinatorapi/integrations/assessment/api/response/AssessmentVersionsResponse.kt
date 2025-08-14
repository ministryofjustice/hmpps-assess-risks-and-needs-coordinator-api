package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response

import java.time.LocalDateTime
import java.util.UUID

typealias AssessmentVersionsResponse =  List<AssessmentVersionResponse>


data class AssessmentVersionResponse(
  val uuid: UUID,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  var tag: String,
  val versionNumber: Int,
)
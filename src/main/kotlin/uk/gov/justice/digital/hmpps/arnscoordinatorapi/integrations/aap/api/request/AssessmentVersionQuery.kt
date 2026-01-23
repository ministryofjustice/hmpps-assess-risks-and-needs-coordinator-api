package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request

import java.time.LocalDateTime
import java.util.UUID

data class AssessmentIdentifier(
  val uuid: UUID,
) {
  private val type = "UUID"
}

data class AssessmentVersionQuery(
  val user: AAPUser,
  val assessmentIdentifier: AssessmentIdentifier,
  val timestamp: LocalDateTime? = null,
) {
  val type: String = "AssessmentVersionQuery"
}

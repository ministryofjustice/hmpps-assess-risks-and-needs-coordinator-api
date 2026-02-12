package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request

import java.util.UUID

data class AssessmentIdentifier(
  val uuid: UUID,
) {
  val type = "UUID"
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request

enum class AssessmentType {
  SAN_SP,
  SP,
  ;

  fun toFlags(): List<String> = when (this) {
    SAN_SP -> listOf("SAN_BETA")
    SP -> emptyList()
  }
}

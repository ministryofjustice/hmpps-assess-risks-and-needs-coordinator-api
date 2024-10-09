package uk.gov.justice.digital.hmpps.arnscoordinatorapi.config

enum class CounterSignOutcome {
  COUNTERSIGNED,
  AWAITING_DOUBLE_COUNTERSIGN,
  DOUBLE_COUNTERSIGNED,
  REJECTED,
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.arnsAssessment

data class SentencePlanResponse(
  val crn: String,
  val nomis: String?,
  val planStatus: String?,
  val goals: List<AssessmentGoal>,
)

data class AssessmentGoal(
  val goalTitle: String,
  val areaOfNeed: String,
  val relatedAreasOfNeed: List<String>,
  val targetDate: String?,
  val goalStatus: String,
  val steps: List<AssessmentStep>,
)

data class AssessmentStep(
  val description: String,
  val status: String,
  val actor: String,
  val statusDate: String,
)

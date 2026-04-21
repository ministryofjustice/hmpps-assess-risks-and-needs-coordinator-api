package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

data class EntityAssociationDetails(
  val oasysAssessmentPk: String,
  val regionPrisonCode: String?,
  val baseVersion: Long,
)

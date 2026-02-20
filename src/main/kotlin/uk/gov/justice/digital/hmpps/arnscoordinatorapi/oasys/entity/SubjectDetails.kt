package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity

import io.swagger.v3.oas.annotations.media.Schema

data class SubjectDetails(
  @Schema(description = "Case Reference Number (CRN) of the offender", example = "X123456")
  val crn: String? = null,

  @Schema(description = "NOMIS ID of the offender", example = "A1234BC")
  val nomisId: String? = null,

  @Schema(description = "Given name (forename) of the offender", example = "John")
  val givenName: String? = null,
)

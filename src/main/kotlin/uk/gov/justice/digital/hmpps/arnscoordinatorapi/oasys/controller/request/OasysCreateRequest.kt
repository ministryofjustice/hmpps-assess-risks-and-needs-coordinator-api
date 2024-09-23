package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Constraints
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails

data class OasysCreateRequest(
  @Schema(
    required = true,
    description = "OASys Assessment PK to create an association for",
    example = "CY/12ZX56",
  )
  @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
  val oasysAssessmentPk: String,

  @Schema(
    description = "(Optional) Provide an old OASys Assessment PK. " +
      "The new OASys Assessment PK will be associated to clones of the previous associated entities",
    example = "CY/12ZX56",
  )
  @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
  val previousOasysAssessmentPk: String? = null,

  @Schema(description = "Region prison code", example = "111111")
  @Size(max = Constraints.REGION_PRISON_CODE_MAX_LENGTH)
  val regionPrisonCode: String? = null,

  @Schema(description = "OASys User Details")
  @Valid
  val userDetails: OasysUserDetails,
)

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Constraints
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails

data class OasysCreateRequest(
  @Schema(
    required = true,
    description = "OASys Assessment PK to create an association for",
    example = "123456",
  )
  @field:Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
  @field:Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
  val oasysAssessmentPk: String,

  @Schema(
    description = "OASys Assessment PK linked to an existing SAN Assessment. " +
      "Used to locate and associate the existing SAN with the new OASys Assessment.",
    example = "123456",
  )
  @field:Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
  @field:Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
  val previousOasysSanPk: String? = null,

  @Schema(
    description = "OASys Assessment PK linked to an existing Sentence Plan. " +
      "Used to locate and associate the existing SP with the new OASys Assessment. ",
    example = "123456",
  )
  @field:Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
  @field:Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
  val previousOasysSpPk: String? = null,

  @Schema(description = "Region prison code", example = "111111")
  @field:Size(max = Constraints.REGION_PRISON_CODE_MAX_LENGTH)
  val regionPrisonCode: String? = null,

  @Schema(description = "Sentence plan type", example = "INITIAL")
  val planType: PlanType,

  @Schema(description = "Assessment type", example = "SAN_SP")
  val assessmentType: AssessmentType,

  @Schema(description = "OASys User Details")
  @field:Valid
  val userDetails: OasysUserDetails,

  @Schema(
    description = "Indicates if this is a new period of supervision for the offender. " +
      "When true, any existing Sentence Plan will be reset (e.g. Agree status cleared).",
    example = "false",
  )
  val newPeriodOfSupervision: Boolean = false,
)

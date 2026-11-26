package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Constraints
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.service.OasysCoordinatorService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysMergeRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysAssociationsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysMessageResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Tag(name = "OASys")
@RequestMapping("\${app.self.endpoints.oasys}")
class OasysController(
  private val oasysCoordinatorService: OasysCoordinatorService,
) {
  @RequestMapping(path = ["/{oasysAssessmentPK}"], method = [RequestMethod.GET])
  @Operation(description = "Get the latest version of entities associated with an OASys Assessment PK")
  @PreAuthorize("hasRole('ROLE_STRENGTHS_AND_NEEDS_OASYS')")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Entities found",
        content = arrayOf(Content(schema = Schema(implementation = OasysGetResponse::class))),
      ),
      ApiResponse(
        responseCode = "404",
        description = "No associated entities were found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun get(
    @Parameter(description = "OASys Assessment PK", required = true, example = "1001")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
    @Valid oasysAssessmentPK: String,
  ): OasysGetResponse = oasysCoordinatorService.get(oasysAssessmentPK)

  @RequestMapping(path = ["/create"], method = [RequestMethod.POST])
  @Operation(description = "Create entities and associate them with an OASys assessment PK")
  @PreAuthorize("hasRole('ROLE_STRENGTHS_AND_NEEDS_OASYS')")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Entities and associations created successfully",
        content = arrayOf(Content(schema = Schema(implementation = OasysVersionedEntityResponse::class))),
      ),
      ApiResponse(
        responseCode = "404",
        description = "Previous association/entity not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "An association already exists for the provided OASys Assessment PK",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun create(
    @RequestBody @Valid request: OasysCreateRequest,
  ): OasysVersionedEntityResponse = oasysCoordinatorService.create(request)

  @RequestMapping(path = ["/merge"], method = [RequestMethod.POST])
  @Operation(description = "Transfer associated entities from one PK to another")
  @PreAuthorize("hasAnyRole('ROLE_STRENGTHS_AND_NEEDS_OASYS')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Entities associated successfully"),
      ApiResponse(
        responseCode = "404",
        description = "Previous association/entity not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "An association already exists for the provided OASys Assessment PK",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun merge(
    @RequestBody @Valid request: OasysMergeRequest,
  ): OasysMessageResponse = oasysCoordinatorService.merge(request)

  @RequestMapping(path = ["/{oasysAssessmentPK}/sign"], method = [RequestMethod.POST])
  @Operation(description = "Signs the latest version of all entities associated with the provided OASys Assessment PK")
  @PreAuthorize("hasAnyRole('ROLE_STRENGTHS_AND_NEEDS_OASYS')")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Entity versions signed successfully",
        content = arrayOf(Content(schema = Schema(implementation = OasysVersionedEntityResponse::class))),
      ),
      ApiResponse(
        responseCode = "404",
        description = "Association/entity not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "An entity could not be signed. See details in error message.",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun sign(
    @Parameter(description = "OASys Assessment PK", required = true, example = "1001")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
    @Valid oasysAssessmentPK: String,
    @RequestBody @Valid request: OasysSignRequest,
  ): OasysVersionedEntityResponse = oasysCoordinatorService.sign(request, oasysAssessmentPK)

  @RequestMapping(path = ["/{oasysAssessmentPK}/counter-sign"], method = [RequestMethod.POST])
  @Operation(description = "Marks the entity version's as counter-signed.")
  @PreAuthorize("hasAnyRole('ROLE_STRENGTHS_AND_NEEDS_OASYS')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Entity versions counter-signed successfully"),
      ApiResponse(
        responseCode = "404",
        description = "Association/entity not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "An entity could not be counter-signed. See details in error message.",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun counterSign(
    @Parameter(description = "OASys Assessment PK", required = true, example = "1001")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
    @Valid oasysAssessmentPK: String,
    @RequestBody @Valid request: OasysCounterSignRequest,
  ): OasysVersionedEntityResponse = oasysCoordinatorService.counterSign(oasysAssessmentPK, request)

  @RequestMapping(path = ["/{oasysAssessmentPK}/lock"], method = [RequestMethod.POST])
  @Operation(description = "Locks the latest version of all associated entities.")
  @PreAuthorize("hasAnyRole('ROLE_STRENGTHS_AND_NEEDS_OASYS')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Entities locked successfully"),
      ApiResponse(
        responseCode = "404",
        description = "Association/entity not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "The latest version of an entity has already been locked. See details in error message.",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun lock(
    @Parameter(description = "OASys Assessment PK", required = true, example = "1001")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
    @Valid
    oasysAssessmentPK: String,
    @RequestBody @Valid
    request: OasysGenericRequest,
  ): OasysVersionedEntityResponse = oasysCoordinatorService.lock(request, oasysAssessmentPK)

  @RequestMapping(path = ["/{oasysAssessmentPK}/rollback"], method = [RequestMethod.POST])
  @Operation(description = "Create a new \"ROLLBACK\" version of specified entities")
  @PreAuthorize("hasAnyRole('ROLE_STRENGTHS_AND_NEEDS_OASYS')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "ROLLBACK version created"),
      ApiResponse(
        responseCode = "404",
        description = "Association/entity not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "Unable to create ROLLBACK for latest entity version",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun rollback(
    @Parameter(description = "OASys Assessment PK", required = true, example = "1001")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
    @Valid oasysAssessmentPK: String,
    @RequestBody @Valid
    request: OasysRollbackRequest,
  ): OasysVersionedEntityResponse = oasysCoordinatorService.rollback(request, oasysAssessmentPK)

  @RequestMapping(path = ["/{oasysAssessmentPK}/soft-delete"], method = [RequestMethod.POST])
  @Operation(description = "Soft-deletes associations for OASys Assessment PK")
  @PreAuthorize("hasAnyRole('ROLE_STRENGTHS_AND_NEEDS_OASYS')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Associations have been soft-deleted"),
      ApiResponse(
        responseCode = "404",
        description = "Association/entity not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "Unable to soft-delete an association that has already been soft-deleted",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun softDelete(
    @Parameter(description = "OASys Assessment PK", required = true, example = "1001")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
    @Valid oasysAssessmentPK: String,
    @RequestBody @Valid request: OasysGenericRequest,
  ): OasysVersionedEntityResponse = oasysCoordinatorService.softDelete(request, oasysAssessmentPK)

  @RequestMapping(path = ["/{oasysAssessmentPK}/undelete"], method = [RequestMethod.POST])
  @Operation(description = "Undeletes associations for OASys Assessment PK")
  @PreAuthorize("hasAnyRole('ROLE_STRENGTHS_AND_NEEDS_OASYS')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Associations have been undeleted"),
      ApiResponse(
        responseCode = "404",
        description = "Association/entity not found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "409",
        description = "No associations are marked as deleted, cannot undelete",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun undelete(
    @Parameter(description = "OASys Assessment PK", required = true, example = "1001")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
    @Valid oasysAssessmentPK: String,
    @RequestBody @Valid request: OasysGenericRequest,
  ): OasysVersionedEntityResponse = oasysCoordinatorService.undelete(request, oasysAssessmentPK)

  @RequestMapping(path = ["/{oasysAssessmentPK}/associations"], method = [RequestMethod.GET])
  @Operation(description = "Return the associations for OASys Assessment PK")
  @PreAuthorize("hasAnyRole('ROLE_STRENGTHS_AND_NEEDS_OASYS')")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Associations returned",
        content = arrayOf(Content(schema = Schema(implementation = OasysAssociationsResponse::class))),
      ),
      ApiResponse(
        responseCode = "404",
        description = "No associations found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun associations(
    @Parameter(description = "OASys Assessment PK", required = true, example = "1001")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Pattern(regexp = "\\d+", message = "Must only contain numeric characters")
    @Valid oasysAssessmentPK: String,
    @RequestParam planVersion: Long?,
    @RequestParam assessmentVersion: Long?,
  ): OasysAssociationsResponse = oasysCoordinatorService.getAssociations(oasysAssessmentPK, planVersion, assessmentVersion)
}

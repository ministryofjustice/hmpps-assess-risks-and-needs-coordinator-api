package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller

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
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Constraints
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.OasysCoordinatorService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysMergeRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysAssociationsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
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
  ): ResponseEntity<*> = when (val result = oasysCoordinatorService.get(oasysAssessmentPK)) {
    is OasysCoordinatorService.GetOperationResult.Success ->
      ResponseEntity.status(HttpStatus.OK).body(result.data)

    is OasysCoordinatorService.GetOperationResult.Failure ->
      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result.errorMessage)

    is OasysCoordinatorService.GetOperationResult.NoAssociations ->
      ResponseEntity.status(HttpStatus.NOT_FOUND).body(result.errorMessage)
  }

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
  ): ResponseEntity<Any> {
    val result = if (request.previousOasysAssessmentPk === null) {
      oasysCoordinatorService.create(request)
    } else {
      oasysCoordinatorService.clone(request)
    }

    return when (result) {
      is OasysCoordinatorService.CreateOperationResult.Success ->
        ResponseEntity.status(HttpStatus.CREATED).body(result.data)

      is OasysCoordinatorService.CreateOperationResult.ConflictingAssociations ->
        ResponseEntity.status(HttpStatus.CONFLICT).body(
          ErrorResponse(
            status = HttpStatus.CONFLICT,
            userMessage = result.errorMessage,
          ),
        )

      is OasysCoordinatorService.CreateOperationResult.Failure ->
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
          ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            userMessage = result.errorMessage,
          ),
        )

      is OasysCoordinatorService.CreateOperationResult.NoAssociations ->
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(
          ErrorResponse(
            status = HttpStatus.NOT_FOUND,
            userMessage = result.errorMessage,
          ),
        )
    }
  }

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
  ): ResponseEntity<Any> = when (val result = oasysCoordinatorService.merge(request)) {
    is OasysCoordinatorService.MergeOperationResult.Success ->
      ResponseEntity.status(HttpStatus.OK).body(result.data)

    is OasysCoordinatorService.MergeOperationResult.NoAssociations ->
      ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.MergeOperationResult.Failure ->
      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.MergeOperationResult.Conflict ->
      ResponseEntity.status(HttpStatus.CONFLICT).body(
        ErrorResponse(
          status = HttpStatus.CONFLICT,
          userMessage = result.errorMessage,
        ),
      )
  }

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
  ): ResponseEntity<Any> = when (val result = oasysCoordinatorService.sign(request, oasysAssessmentPK)) {
    is OasysCoordinatorService.SignOperationResult.Success ->
      ResponseEntity.status(HttpStatus.OK).body(result.data)

    is OasysCoordinatorService.SignOperationResult.NoAssociations ->
      ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.SignOperationResult.Failure ->
      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.SignOperationResult.Conflict ->
      ResponseEntity.status(HttpStatus.CONFLICT).body(
        ErrorResponse(
          status = HttpStatus.CONFLICT,
          userMessage = result.errorMessage,
        ),
      )
  }

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
  ): ResponseEntity<Any> = when (val result = oasysCoordinatorService.counterSign(oasysAssessmentPK, request)) {
    is OasysCoordinatorService.CounterSignOperationResult.Failure -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.CounterSignOperationResult.NoAssociations -> ResponseEntity.status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.CounterSignOperationResult.Success -> ResponseEntity.status(HttpStatus.OK)
      .body(result.data)

    is OasysCoordinatorService.CounterSignOperationResult.Conflict -> ResponseEntity.status(HttpStatus.CONFLICT).body(
      ErrorResponse(
        status = HttpStatus.CONFLICT,
        userMessage = result.errorMessage,
      ),
    )
  }

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
  ): ResponseEntity<Any> = when (val result = oasysCoordinatorService.lock(request, oasysAssessmentPK)) {
    is OasysCoordinatorService.LockOperationResult.Success ->
      ResponseEntity.status(HttpStatus.OK).body(result.data)

    is OasysCoordinatorService.LockOperationResult.NoAssociations ->
      ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.LockOperationResult.Failure ->
      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.LockOperationResult.Conflict ->
      ResponseEntity.status(HttpStatus.CONFLICT).body(
        ErrorResponse(
          status = HttpStatus.CONFLICT,
          userMessage = result.errorMessage,
        ),
      )
  }

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
  ): ResponseEntity<Any> = when (val result = oasysCoordinatorService.rollback(request, oasysAssessmentPK)) {
    is OasysCoordinatorService.RollbackOperationResult.Success ->
      ResponseEntity.status(HttpStatus.OK).body(result.data)

    is OasysCoordinatorService.RollbackOperationResult.NoAssociations ->
      ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.RollbackOperationResult.Failure ->
      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.RollbackOperationResult.Conflict ->
      ResponseEntity.status(HttpStatus.CONFLICT).body(
        ErrorResponse(
          status = HttpStatus.CONFLICT,
          userMessage = result.errorMessage,
        ),
      )
  }

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
  ): ResponseEntity<Any> = when (val result = oasysCoordinatorService.softDelete(request, oasysAssessmentPK)) {
    is OasysCoordinatorService.SoftDeleteOperationResult.Success ->
      ResponseEntity.status(HttpStatus.OK).body(result.data)

    is OasysCoordinatorService.SoftDeleteOperationResult.NoAssociations ->
      ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.SoftDeleteOperationResult.Failure ->
      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.SoftDeleteOperationResult.Conflict ->
      ResponseEntity.status(HttpStatus.CONFLICT).body(
        ErrorResponse(
          status = HttpStatus.CONFLICT,
          userMessage = result.errorMessage,
        ),
      )
  }

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
  ): ResponseEntity<Any> = when (val result = oasysCoordinatorService.undelete(request, oasysAssessmentPK)) {
    is OasysCoordinatorService.UndeleteOperationResult.Success ->
      ResponseEntity.status(HttpStatus.OK).body(result.data)

    is OasysCoordinatorService.UndeleteOperationResult.NoAssociations ->
      ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.UndeleteOperationResult.Failure ->
      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR,
          userMessage = result.errorMessage,
        ),
      )

    is OasysCoordinatorService.UndeleteOperationResult.Conflict ->
      ResponseEntity.status(HttpStatus.CONFLICT).body(
        ErrorResponse(
          status = HttpStatus.CONFLICT,
          userMessage = result.errorMessage,
        ),
      )
  }

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
  ): ResponseEntity<Any> = when (val result = oasysCoordinatorService.getAssociations(oasysAssessmentPK)) {
    is OasysCoordinatorService.GetOperationResult.Failure ->
      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result.errorMessage)

    is OasysCoordinatorService.GetOperationResult.NoAssociations ->
      ResponseEntity.status(HttpStatus.NOT_FOUND).body(result.errorMessage)

    is OasysCoordinatorService.GetOperationResult.Success ->
      ResponseEntity.status(HttpStatus.OK).body(result.data)
  }
}

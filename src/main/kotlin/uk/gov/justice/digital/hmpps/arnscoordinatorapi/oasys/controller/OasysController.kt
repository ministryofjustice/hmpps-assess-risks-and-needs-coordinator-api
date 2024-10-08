package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysMessageResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

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
    @Parameter(description = "OASys Assessment PK", required = true, example = "oasys-pk-goes-here")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Valid oasysAssessmentPK: String,
  ): ResponseEntity<*> {
    return when (val result = oasysCoordinatorService.get(oasysAssessmentPK)) {
      is OasysCoordinatorService.GetOperationResult.Success ->
        ResponseEntity.status(HttpStatus.OK).body(result.data)
      is OasysCoordinatorService.GetOperationResult.Failure ->
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result.errorMessage)
      is OasysCoordinatorService.GetOperationResult.NoAssociations ->
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(result.errorMessage)
    }
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
    /**
     * TODO: Implement logic for 2 separate behaviours
     *  1. DONE!
     *  2. From Existing
     *    2.1. Using existing OASys Assessment PK, create a clone from an existing entity in SAN, SP
     *    2.2. Store clone entity UUID against the new OASys Assessment PK
     *    2.3. Return entity UUIDs and version numbers
     *  If any failures, rollback and return error to OASys
     */
    if (request.previousOasysAssessmentPk === null) {
      return when (val result = oasysCoordinatorService.create(request)) {
        is OasysCoordinatorService.CreateOperationResult.Success ->
          ResponseEntity.status(HttpStatus.CREATED).body(result.data)
        is OasysCoordinatorService.CreateOperationResult.ConflictingAssociations ->
          ResponseEntity.status(HttpStatus.CONFLICT).body(
            "An association already exists for the provided OASys Assessment PK: ${request.oasysAssessmentPk}. \n" +
              "Error details: ${result.errorMessage}",
          )
        is OasysCoordinatorService.CreateOperationResult.Failure ->
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result.errorMessage)
      }
    } else {
      return ResponseEntity.status(HttpStatus.OK).body("IMPLEMENT THIS LATER")
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
  ): OasysMessageResponse {
    /**
     * TODO: Implement logic to merge two or more associated OASys Assessment PKs together
     *  1. Loop over each OASys Assessment PK pair
     *  2. Update all associations in DB that have old PK, replace with new PK
     */
    return OasysMessageResponse("Successfully processed all ${request.merge.size} merge elements")
  }

  @RequestMapping(path = ["/{oasysAssessmentPK}/sign"], method = [RequestMethod.POST])
  @Operation(description = "Signs the latest version of all entities associated with the provided OASys Assessment PK")
  @PreAuthorize("hasAnyRole('ROLE_STRENGTHS_AND_NEEDS_OASYS')")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Entity versions signed successfully"),
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
    @Parameter(description = "OASys Assessment PK", required = true, example = "oasys-pk-goes-here")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Valid oasysAssessmentPK: String,
    @RequestBody @Valid request: OasysSignRequest,
  ): OasysVersionedEntityResponse {
    /**
     * TODO: Implement logic to entities for self-signing
     *  1. Find all associations for a PK in the DB
     *  2. Contact each service's relevant API to self-sign their latest entity, API will return a version number
     *  3. Return combination of entity UUIDs and their signed version number
     */
    return OasysVersionedEntityResponse(
      sanAssessmentId = UUID.randomUUID(),
      sanAssessmentVersion = 0,
      sentencePlanId = UUID.randomUUID(),
      sentencePlanVersion = 0,
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
    @Parameter(description = "OASys Assessment PK", required = true, example = "oasys-pk-goes-here")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Valid oasysAssessmentPK: String,
    @RequestBody @Valid request: OasysCounterSignRequest,
  ): OasysVersionedEntityResponse {
    /**
     * TODO: Implement logic to countersign all entities
     *  1. Find all associations for a PK in the DB
     *  2. Contact each service's relevant API to sign that entity with the specific tag, API will return a version number
     *  3. Return combination of entity UUIDs and their locked version number
     */
    return OasysVersionedEntityResponse(
      sanAssessmentId = UUID.randomUUID(),
      sanAssessmentVersion = 0,
      sentencePlanId = UUID.randomUUID(),
      sentencePlanVersion = 0,
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
    @Parameter(description = "OASys Assessment PK", required = true, example = "oasys-pk-goes-here")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Valid
    oasysAssessmentPK: String,
    @RequestBody @Valid
    request: OasysGenericRequest,
  ): ResponseEntity<Any> {
    return when (val result = oasysCoordinatorService.lock(request, oasysAssessmentPK)) {
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
    @Parameter(description = "OASys Assessment PK", required = true, example = "oasys-pk-goes-here")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Valid oasysAssessmentPK: String,
    @RequestBody @Valid
    request: OasysRollbackRequest,
  ): OasysVersionedEntityResponse {
    /**
     * TODO: Implement logic to rollback the sign state of an entity
     *  1. Find all associations for a PK in the DB
     *  2. Contact each service's relevant API to rollback that entity with the provided version number,
     *    API will return a version number
     *  3. Return combination of entity UUIDs and their locked version number
     */

    return OasysVersionedEntityResponse(
      sanAssessmentId = UUID.randomUUID(),
      sanAssessmentVersion = 0,
      sentencePlanId = UUID.randomUUID(),
      sentencePlanVersion = 0,
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
    @Parameter(description = "OASys Assessment PK", required = true, example = "oasys-pk-goes-here")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Valid oasysAssessmentPK: String,
    @RequestBody @Valid request: OasysGenericRequest,
  ): OasysMessageResponse {
    /**
     * TODO: Implement logic for soft-deleting an association
     *  1. Find all associations for a PK in the DB
     *  2. Mark each association as deleted
     */
    return OasysMessageResponse("Successfully soft-deleted associations for OASys assessment PK $oasysAssessmentPK")
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
    @Parameter(description = "OASys Assessment PK", required = true, example = "oasys-pk-goes-here")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Valid oasysAssessmentPK: String,
    @RequestBody @Valid request: OasysGenericRequest,
  ): OasysVersionedEntityResponse {
    /**
     * TODO: Implement logic for un-deleting an association
     *  1. Find all associations for a PK in the DB
     *  2. Mark each association as not-deleted
     */
    return OasysVersionedEntityResponse(
      sanAssessmentId = UUID.randomUUID(),
      sanAssessmentVersion = 0,
      sentencePlanId = UUID.randomUUID(),
      sentencePlanVersion = 0,
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
    @Parameter(description = "OASys Assessment PK", required = true, example = "oasys-pk-goes-here")
    @PathVariable
    @Size(min = Constraints.OASYS_PK_MIN_LENGTH, max = Constraints.OASYS_PK_MAX_LENGTH)
    @Valid oasysAssessmentPK: String,
  ): ResponseEntity<Any> {
    return when (val result = oasysCoordinatorService.getAssociations(oasysAssessmentPK)) {
      is OasysCoordinatorService.GetOperationResult.Failure ->
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result.errorMessage)
      is OasysCoordinatorService.GetOperationResult.NoAssociations ->
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(result.errorMessage)
      is OasysCoordinatorService.GetOperationResult.Success ->
        ResponseEntity.status(HttpStatus.OK).body(result.data)
    }
  }
}

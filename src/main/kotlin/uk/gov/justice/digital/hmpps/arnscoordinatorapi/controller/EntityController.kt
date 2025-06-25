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
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.OasysCoordinatorService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@Tag(name = "Entity")
@Validated
@RequestMapping("entity")
class EntityController(
  private val oasysCoordinatorService: OasysCoordinatorService,
) {
  @RequestMapping(path = ["/versions/{entityUuid}"], method = [RequestMethod.GET])
  @Operation(description = "Get the list of Assessment AND Sentence Plan versions for a given Assessment or Sentence Plan ID")
  @PreAuthorize("hasAnyRole('ROLE_SENTENCE_PLAN_READ','ROLE_STRENGTHS_AND_NEEDS_READ')")
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
  fun getVersionsByEntityId(
    @Parameter(description = "Entity UUID. SAN or SP Entity Id", required = true, example = "90a71d16-fecd-4e1a-85b9-98178bf0f8d0")
    @PathVariable
    @Valid entityUuid: UUID,
  ): ResponseEntity<*> = when (val result = oasysCoordinatorService.getVersionsByEntityId(entityUuid)) {
    is OasysCoordinatorService.GetOperationResult.Success ->
      ResponseEntity.status(HttpStatus.OK).body(result.data)

    is OasysCoordinatorService.GetOperationResult.Failure ->
      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result.errorMessage)

    is OasysCoordinatorService.GetOperationResult.NoAssociations ->
      ResponseEntity.status(HttpStatus.NOT_FOUND).body(result.errorMessage)
  }

  @RequestMapping(path = ["/{entityUuid}/{entityType}"], method = [RequestMethod.GET])
  @Operation(description = "Get the latest version of the provided entity type associated with the OASys Assessment PK linked to the provided entity id")
  @PreAuthorize("hasAnyRole('ROLE_STRENGTHS_AND_NEEDS_OASYS','ROLE_SENTENCE_PLAN_READ')")
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
  fun getByEntityId(
    @Parameter(description = "Entity UUID. SAN or SP Entity Id", required = true, example = "90a71d16-fecd-4e1a-85b9-98178bf0f8d0")
    @PathVariable
    @Valid entityUuid: UUID,
    @Parameter(description = "Entity Type. ASSESSMENT or PLAN", required = true, example = "ASSESSMENT")
    @PathVariable
    @Pattern(regexp = "ASSESSMENT|PLAN")
    @Valid entityType: String,
  ): ResponseEntity<*> = when (val result = oasysCoordinatorService.getByEntityId(entityUuid, EntityType.valueOf(entityType))) {
    is OasysCoordinatorService.GetOperationResult.Success ->
      ResponseEntity.status(HttpStatus.OK).body(result.data)

    is OasysCoordinatorService.GetOperationResult.Failure ->
      ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result.errorMessage)

    is OasysCoordinatorService.GetOperationResult.NoAssociations ->
      ResponseEntity.status(HttpStatus.NOT_FOUND).body(result.errorMessage)
  }
}

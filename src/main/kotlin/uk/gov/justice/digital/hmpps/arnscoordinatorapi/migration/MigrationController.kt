package uk.gov.justice.digital.hmpps.arnscoordinatorapi.migration

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.util.UUID

data class VersionMapping(val version: Long, val createdAt: LocalDateTime, val event: OasysEvent)
data class MigrateAssociationRequest(
  val mappings: List<VersionMapping>,
  val entityTypeFrom: EntityType,
  val entityTypeTo: EntityType,
  val entityUuid: UUID,
)

@RestController
@Tag(name = "OASys - Migration")
@RequestMapping("\${app.self.endpoints.oasys}")
class MigrationController(val migrationService: MigrationService) {
  @RequestMapping(path = ["/{oasysAssessmentPK}/migrate-associations"], method = [RequestMethod.POST])
  @Operation(description = "Migrates an association")
  @PreAuthorize("hasRole('ROLE_MIGRATE_SENTENCE_PLAN')")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Assessment association migrated",
        content = arrayOf(Content(schema = Schema(implementation = OasysGetResponse::class))),
      ),
      ApiResponse(
        responseCode = "404",
        description = "No associated was found",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = arrayOf(Content(schema = Schema(implementation = ErrorResponse::class))),
      ),
    ],
  )
  fun migrateAssociation(
    @PathVariable oasysAssessmentPK: String,
    @RequestBody request: MigrateAssociationRequest,
  ) = migrationService.migrateAssociation(oasysAssessmentPK, request)
}

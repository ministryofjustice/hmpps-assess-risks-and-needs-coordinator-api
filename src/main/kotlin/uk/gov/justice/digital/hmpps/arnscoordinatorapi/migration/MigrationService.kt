package uk.gov.justice.digital.hmpps.arnscoordinatorapi.migration

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.service.OasysVersionService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

class AssociationNotFoundException(
  val entityUuid: UUID,
) : RuntimeException("Association '$entityUuid' not found") {
  fun intoResponse(): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(HttpStatus.NOT_FOUND)
    .body(
      ErrorResponse(
        userMessage = message.orEmpty(),
        developerMessage = "No resource found for $entityUuid",
        status = HttpStatus.NOT_FOUND,
      ),
    )
}

@Service
class MigrationService(
  private val oasysAssociationsService: OasysAssociationsService,
  private val oasysVersionService: OasysVersionService,
) {
  fun migrateAssociation(request: MigrateAssociationRequest) {
    oasysAssociationsService.findAllOfAnyKindIncludingDeleted(request.entityUuidFrom)
      .mapIndexed { index, association ->
        OasysAssociation(
          createdAt = association.createdAt,
          entityType = request.entityTypeTo,
          entityUuid = request.entityUuidTo,
          oasysAssessmentPk = association.oasysAssessmentPk,
          regionPrisonCode = association.regionPrisonCode,
          deleted = association.deleted,
          baseVersion = association.baseVersion,
        ).also {
          when (oasysAssociationsService.storeAssociation(it)) {
            is OperationResult.Failure<*> -> throw IllegalStateException("Failed to persist new association with entity UUID: ${request.entityUuidTo} ($index)")
            else -> {
              log.info("Created new association for entity UUID: ${request.entityUuidTo} ($index)")
            }
          }
        }
      }
      .firstOrNull()
      ?: throw AssociationNotFoundException(request.entityUuidFrom)

    request.mappings.map { versionMapping ->
      OasysVersionEntity(
        createdAt = versionMapping.createdAt,
        createdBy = versionMapping.event,
        updatedAt = versionMapping.createdAt,
        version = versionMapping.version,
        entityUuid = request.entityUuidTo,
        deleted = false,
      )
    }.run(oasysVersionService::saveAll)
      .also { log.info("Migrated ${it.size} versions for  entity UUID: ${request.entityUuidTo}") }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

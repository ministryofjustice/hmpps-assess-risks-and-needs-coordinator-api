package uk.gov.justice.digital.hmpps.arnscoordinatorapi.migration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.service.OasysVersionService
import java.time.LocalDateTime

@Service
class MigrationService(
  private val oasysAssociationsService: OasysAssociationsService,
  private val oasysVersionService: OasysVersionService,
) {
  fun migrateAssociation(request: MigrateAssociationRequest) {
    oasysAssociationsService.findByEntityId(request.entityUuid)
      .mapIndexed { index, association ->
        OasysAssociation(
          createdAt = association.createdAt,
          entityType = request.entityTypeTo,
          entityUuid = request.entityUuid,
          oasysAssessmentPk = association.oasysAssessmentPk,
          regionPrisonCode = association.regionPrisonCode,
          deleted = false,
          baseVersion = association.baseVersion,
        ).also {
          when (oasysAssociationsService.storeAssociation(it)) {
            is OperationResult.Failure<*> -> throw IllegalStateException("Failed to persist new association with entity UUID: ${request.entityUuid} ($index)")
            else -> {
              log.info("Created new association for entity UUID: ${request.entityUuid} ($index)")
            }
          }
        }
      }
      .firstOrNull()
      ?: throw IllegalStateException("Unable to find association with entity UUID: ${request.entityUuid}")

    request.mappings.map { versionMapping ->
      OasysVersionEntity(
        createdAt = versionMapping.createdAt,
        createdBy = versionMapping.event,
        updatedAt = LocalDateTime.now(),
        version = versionMapping.version,
        entityUuid = request.entityUuid,
        deleted = false,
      )
    }.run(oasysVersionService::saveAll)
      .also { log.info("Migrated ${it.size} versions for  entity UUID: ${request.entityUuid}") }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

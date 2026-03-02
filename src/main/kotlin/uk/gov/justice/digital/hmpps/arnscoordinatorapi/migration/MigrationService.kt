package uk.gov.justice.digital.hmpps.arnscoordinatorapi.migration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.service.OasysVersionService
import java.time.LocalDateTime
import java.util.UUID

data class VersionMapping(val version: Long, val createdAt: LocalDateTime, val event: OasysEvent)
data class MigrateAssociationRequest(
  val mappings: List<VersionMapping>,
  val entityTypeFrom: EntityType,
  val entityTypeTo: EntityType,
  val entityUuid: UUID,
)

@Service
class MigrationService(
  private val oasysAssociationsService: OasysAssociationsService,
  private val oasysVersionService: OasysVersionService,
) {
  fun migrateAssociation(oasysAssessmentPK: String, request: MigrateAssociationRequest) {
    val existingAssociation =
      oasysAssociationsService.findAssociationsByPkAndType(oasysAssessmentPK, listOf(request.entityTypeFrom))
        .firstOrNull()
        ?: throw IllegalStateException("Unable to find association by PK: $oasysAssessmentPK")

    OasysAssociation(
      createdAt = existingAssociation.createdAt,
      entityType = request.entityTypeTo,
      entityUuid = request.entityUuid,
      oasysAssessmentPk = oasysAssessmentPK,
      regionPrisonCode = existingAssociation.regionPrisonCode,
      deleted = false,
      baseVersion = existingAssociation.baseVersion,
    ).also {
      when (oasysAssociationsService.storeAssociation(it)) {
        is OperationResult.Failure<*> -> throw IllegalStateException("Failed to persist new association for : $oasysAssessmentPK")
        else -> {
          log.info("Created new association for $oasysAssessmentPK")
        }
      }
    }

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
      .also { log.info("Migrated ${it.size} versions for $oasysAssessmentPK") }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

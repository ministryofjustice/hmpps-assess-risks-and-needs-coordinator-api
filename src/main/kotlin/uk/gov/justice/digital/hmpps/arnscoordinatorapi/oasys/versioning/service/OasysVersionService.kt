package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionRepository
import java.util.UUID

@Service
class OasysVersionService(
  private val repository: OasysVersionRepository,
) {
  fun createVersionFor(event: OasysEvent, entityUuid: UUID): OasysVersionEntity {
    val previous = repository.findTopByEntityUuidOrderByVersionDesc(entityUuid)

    return OasysVersionEntity(
      createdBy = event,
      version = previous?.version?.plus(1) ?: 0,
      entityUuid = entityUuid,
    ).run(repository::save)
  }

  fun updateVersion(event: OasysEvent, entityUuid: UUID, version: Long): OasysVersionEntity? = repository.findByEntityUuidAndVersion(entityUuid, version)
    ?.apply {
      createdBy = event
      updatedAt = Clock.now()
    }
    ?.run(repository::save)

  fun softDeleteVersions(entityUuid: UUID, from: Long, to: Long?): OasysVersionEntity = repository.findAllByEntityUuidAndVersionBetween(
    entityUuid,
    from,
    to ?: getLatestVersionNumberForEntityUuid(entityUuid),
  )
    .ifEmpty { throw Error("No versions found for entity $entityUuid between $from to $to") }
    .map { it.apply { deleted = true } }
    .let { updatedVersions -> repository.saveAll(updatedVersions) }
    .last()

  fun undeleteVersions(entityUuid: UUID, from: Long, to: Long?): OasysVersionEntity = repository.findAllDeletedByEntityUuidAndVersionBetween(
    entityUuid,
    from,
    to ?: getLatestVersionNumberForEntityUuid(entityUuid),
  )
    .ifEmpty { throw Error("No versions found for entity $entityUuid between $from to $to") }
    .map { it.apply { deleted = false } }
    .let { updatedVersions -> repository.saveAll(updatedVersions) }
    .last()

  fun fetchAllForEntityUuid(entityUuid: UUID) = repository.findAllByEntityUuid(entityUuid)
  fun getLatestVersionNumberForEntityUuid(entityUuid: UUID) = repository.findLatestVersionForEntityUuid(entityUuid)
}

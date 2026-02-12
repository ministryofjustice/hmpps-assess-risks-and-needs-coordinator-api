package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionRepository
import java.time.ZoneOffset
import java.util.UUID

@Service
class OasysVersionService(
  private val repository: OasysVersionRepository,
  private val clock: Clock,
) {
  fun createVersionFor(event: OasysEvent, entityUuid: UUID): OasysVersionEntity = OasysVersionEntity(
    createdBy = event,
    version = getLatestVersionNumber(),
    entityUuid = entityUuid,
  ).run(repository::save)

  fun updateVersion(event: OasysEvent, entityUuid: UUID, version: Long): OasysVersionEntity = repository.findByEntityUuidAndVersion(entityUuid, version)
    ?.apply {
      createdBy = event
      updatedAt = clock.now()
    }
    ?.run(repository::save)
    ?: throw Error("Unable to update version, no previous version found for $entityUuid")

  fun softDeleteVersions(entityUuid: UUID, from: Long, to: Long?): OasysVersionEntity {
    val toVersion = to ?: getLatestVersionNumber()
    val versions = repository.findAllByEntityUuidAndVersionBetween(entityUuid, from, toVersion)

    if (versions.isEmpty()) {
      val errorMessage = "No versions found for entity $entityUuid between $from to $toVersion"
      log.warn(errorMessage)
      throw Error(errorMessage)
    }

    return versions
      .map { it.apply { deleted = true } }
      .let { updatedVersions -> repository.saveAll(updatedVersions) }
      .last()
  }

  fun undeleteVersions(entityUuid: UUID, from: Long, to: Long?): OasysVersionEntity {
    val toVersion = to ?: getLatestVersionNumber()
    val versions = repository.findAllDeletedByEntityUuidAndVersionBetween(entityUuid, from, toVersion)

    if (versions.isEmpty()) {
      val errorMessage = "No deleted versions found for entity $entityUuid between $from to $toVersion"
      log.warn(errorMessage)
      throw Error(errorMessage)
    }

    return versions
      .map { it.apply { deleted = false } }
      .let { updatedVersions -> repository.saveAll(updatedVersions) }
      .last()
  }

  fun fetchAllForEntityUuid(entityUuid: UUID) = repository.findAllByEntityUuid(entityUuid)

  fun getLatestVersionNumber() = clock.now().toInstant(ZoneOffset.UTC).toEpochMilli()

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

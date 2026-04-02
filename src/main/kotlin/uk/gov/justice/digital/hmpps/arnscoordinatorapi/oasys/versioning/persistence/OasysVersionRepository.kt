package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OasysVersionRepository : JpaRepository<OasysVersionEntity, Long> {
  fun findTopByEntityUuidOrderByVersionDesc(
    entityUuid: UUID,
  ): OasysVersionEntity?

  fun findByEntityUuidAndVersion(entityUuid: UUID, version: Long): OasysVersionEntity?

  fun findAllByEntityUuid(
    entityUuid: UUID,
  ): List<OasysVersionEntity>

  @Query(
    """
    SELECT *
    FROM coordinator.oasys_version
    WHERE entity_uuid = :entityUuid
    AND version BETWEEN :fromVersion AND :toVersion
    AND deleted = true
  """,
    nativeQuery = true,
  )
  fun findAllDeletedByEntityUuidAndVersionBetween(entityUuid: UUID, fromVersion: Long, toVersion: Long): List<OasysVersionEntity>
  fun findAllByEntityUuidAndVersionBetween(entityUuid: UUID, fromVersion: Long, toVersion: Long): List<OasysVersionEntity>
}

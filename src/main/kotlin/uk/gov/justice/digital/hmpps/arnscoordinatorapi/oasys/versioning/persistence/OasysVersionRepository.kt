package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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
        SELECT COALESCE(MAX(v.version), 0)
        FROM OasysVersionEntity v
        WHERE v.entityUuid = :entityUuid
    """,
  )
  fun findLatestVersionForEntityUuid(
    @Param("entityUuid") entityUuid: UUID,
  ): Long

  @Query(
    """
    SELECT e
    FROM coordinator.oasys_version e
    WHERE e.entity_uuid = :entityUuid
    AND e.version BETWEEN :fromVersion AND :toVersion
    AND e.deleted
  """,
    nativeQuery = true,
  )
  fun findAllDeletedByEntityUuidAndVersionBetween(entityUuid: UUID, fromVersion: Long, toVersion: Long): List<OasysVersionEntity>
  fun findAllByEntityUuidAndVersionBetween(entityUuid: UUID, fromVersion: Long, toVersion: Long): List<OasysVersionEntity>
}

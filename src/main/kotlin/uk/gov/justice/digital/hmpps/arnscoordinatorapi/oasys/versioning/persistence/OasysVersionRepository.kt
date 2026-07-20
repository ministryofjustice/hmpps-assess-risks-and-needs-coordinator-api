package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OasysVersionRepository : JpaRepository<OasysVersionEntity, Long> {
  /**
   * Pages through every version row, including soft deleted ones.
   * Paging is by `(entity_uuid, version)`, which never changes once a row exists, so concurrent
   * writes can't move a row between pages.
   */
  @Query(
    value = """
      SELECT * FROM coordinator.oasys_version
      WHERE entity_uuid > :lastEntityUuid
         OR (entity_uuid = :lastEntityUuid AND version > :lastVersion)
      ORDER BY entity_uuid, version
      LIMIT :limit
    """,
    nativeQuery = true,
  )
  fun findNextPageIncludingDeleted(lastEntityUuid: UUID, lastVersion: Long, limit: Int): List<OasysVersionEntity>

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
    AND version >= :fromVersion AND version < :toVersion
    AND deleted = true
  """,
    nativeQuery = true,
  )
  fun findAllDeletedByEntityUuidAndVersionBetween(entityUuid: UUID, fromVersion: Long, toVersion: Long): List<OasysVersionEntity>
  fun findAllByEntityUuidAndVersionGreaterThanEqualAndVersionLessThan(entityUuid: UUID, fromVersion: Long, toVersion: Long): List<OasysVersionEntity>
}

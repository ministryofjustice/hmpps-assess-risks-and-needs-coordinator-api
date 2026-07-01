package uk.gov.justice.digital.hmpps.arnscoordinatorapi.events

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.EntityAssociationDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionRepository
import java.util.UUID
import javax.sql.DataSource
import kotlin.concurrent.thread

/**
 * Backfill that replays every existing `oasys_version` row to the coordinator SQS queue as
 * an `OASYS_VERSION_EVENT`, so the view-api can build snapshots for pre-existing history. Disabled
 * by default. Enabled via `app.historical-events.enabled`. Single execution across multiple
 * potential replicas is enforced with a Postgres advisory lock.
 */
@Component
@ConditionalOnProperty(prefix = "app.historical-events", name = ["enabled"], havingValue = "true")
class HistoricalEventsReplayJob(
  private val versionRepository: OasysVersionRepository,
  private val associationsService: OasysAssociationsService,
  private val publisher: OasysEventPublisher,
  private val dataSource: DataSource,
) {
  @EventListener(ApplicationReadyEvent::class)
  fun onApplicationReady() {
    // Run off the startup thread so readiness/liveness probes are not blocked by the backfill.
    thread(name = "historical-events-replay", isDaemon = true) { runWithAdvisoryLock() }
  }

  /**
   * Acquires a session level advisory lock. Only the pod that wins th e lock runs the replay,
   * the others log and skip.
   */
  internal fun runWithAdvisoryLock() {
    try {
      dataSource.connection.use { lockConnection ->
        val acquired = lockConnection.prepareStatement("SELECT pg_try_advisory_lock(?)").use { ps ->
          ps.setLong(1, ADVISORY_LOCK_KEY)
          ps.executeQuery().use { rs -> rs.next() && rs.getBoolean(1) }
        }

        if (!acquired) {
          log.info("Historical events replay: advisory lock held by another instance, skipping")
          return
        }

        try {
          replayAll()
        } finally {
          lockConnection.prepareStatement("SELECT pg_advisory_unlock(?)").use { ps ->
            ps.setLong(1, ADVISORY_LOCK_KEY)
            ps.executeQuery().use { it.next() }
          }
        }
      }
    } catch (ex: Exception) {
      log.error("Historical events replay aborted with an unexpected error", ex)
    }
  }

  internal fun replayAll() {
    log.info("Historical events replay starting")

    // Cache association resolution across pages so we don't re-query assoiatied details across page boundaries
    val associationCache = HashMap<UUID, EntityAssociationDetails?>()

    var lastEntityUuid = ZERO_UUID
    var lastVersion = Long.MIN_VALUE
    var published = 0
    var skipped = 0
    var failed = 0

    while (true) {
      val page = versionRepository.findNextPageIncludingDeleted(lastEntityUuid, lastVersion, BATCH_SIZE)
      if (page.isEmpty()) break

      resolveAssociationsForPage(page, associationCache)

      for (row in page) {
        try {
          val association = associationCache[row.entityUuid]
          if (association == null) {
            skipped++
            log.warn(
              "No resolvable association/PK for entity {} version {}; skipping",
              row.entityUuid,
              row.version,
            )
            continue
          }
          publisher.publish(buildEvent(row, association))
          published++
        } catch (ex: Exception) {
          failed++
          log.error("Failed to publish historical event for entity {} version {}", row.entityUuid, row.version, ex)
        }
      }

      val last = page.last()
      lastEntityUuid = last.entityUuid
      lastVersion = last.version
      log.info("Historical events replay progress: published={} skipped={} failed={}", published, skipped, failed)
    }

    log.info("Historical events replay complete: published={} skipped={} failed={}", published, skipped, failed)
  }

  private fun resolveAssociationsForPage(
    page: List<OasysVersionEntity>,
    cache: HashMap<UUID, EntityAssociationDetails?>,
  ) {
    val unresolved = page.map { it.entityUuid }.filter { it !in cache }.toSet()
    if (unresolved.isEmpty()) return

    // Batch lookup (latest non-deleted AAP_PLAN/ASSESSMENT association per entity)
    val latest = associationsService.findLatestAssociationDetailsByEntityIds(unresolved)
    for (entityUuid in unresolved) {
      cache[entityUuid] = latest[entityUuid] ?: resolveFallback(entityUuid)
    }
  }

  /**
   * Fallback for entities whose associations are all soft deleted: take the latest association of
   * any kind/state. Returns null when there is still no usable assessment, so the row is skipped
   * rather than published.
   */
  private fun resolveFallback(entityUuid: UUID): EntityAssociationDetails? = associationsService
    .findAllOfAnyKindIncludingDeleted(entityUuid)
    .maxByOrNull { it.createdAt }
    ?.let { association ->
      association.oasysAssessmentPk?.let { pk ->
        EntityAssociationDetails(
          oasysAssessmentPk = pk,
          regionPrisonCode = association.regionPrisonCode,
          baseVersion = association.baseVersion,
        )
      }
    }

  private fun buildEvent(row: OasysVersionEntity, association: EntityAssociationDetails) = CoordinatorEvent(
    eventType = EventType.OASYS_VERSION_EVENT,
    entityType = AAP_PLAN_ENTITY_TYPE,
    entityUuid = row.entityUuid,
    occurredAt = row.updatedAt,
    message = VersionPayload(
      version = row.version,
      oasysEvent = row.createdBy.toEventsOasysEvent(),
      incrementedAt = row.updatedAt,
      deleted = row.deleted,
      association = AssociationPayload(
        oasysAssessmentPk = association.oasysAssessmentPk,
        regionPrisonCode = association.regionPrisonCode,
        baseVersion = association.baseVersion,
      ),
    ),
  )

  companion object {
    private val log = LoggerFactory.getLogger(HistoricalEventsReplayJob::class.java)

    private const val AAP_PLAN_ENTITY_TYPE = "AAP_PLAN"
    private const val BATCH_SIZE = 1000

    // Arbitrary constant identifying the single execution lock for this replay.
    private const val ADVISORY_LOCK_KEY = 31_72_00_01L

    private val ZERO_UUID: UUID = UUID(0L, 0L)
  }
}

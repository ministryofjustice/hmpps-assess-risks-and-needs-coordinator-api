package uk.gov.justice.digital.hmpps.arnscoordinatorapi.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.EntityAssociationDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionRepository
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent as PersistenceOasysEvent

class HistoricalEventsReplayJobTest {

  private val versionRepository = mock<OasysVersionRepository>()
  private val associationsService = mock<OasysAssociationsService>()
  private val publisher = mock<OasysEventPublisher>()
  private val dataSource = mock<DataSource>()

  private val job = HistoricalEventsReplayJob(versionRepository, associationsService, publisher, dataSource)

  private val entityUuid = UUID.fromString("00000001-1111-1111-1111-000000000001")
  private val createdAt = LocalDateTime.of(2026, 6, 10, 9, 0, 0)
  private val updatedAt = LocalDateTime.of(2026, 6, 12, 14, 30, 0)

  private fun versionRow(
    version: Long,
    event: PersistenceOasysEvent,
    deleted: Boolean = false,
    uuid: UUID = entityUuid,
    updated: LocalDateTime = updatedAt,
  ) = OasysVersionEntity(
    createdAt = createdAt,
    createdBy = event,
    updatedAt = updated,
    version = version,
    entityUuid = uuid,
    deleted = deleted,
  )

  @Test
  fun `replayAll publishes one correctly-mapped event per version row`() {
    val rows = listOf(
      versionRow(version = 100, event = PersistenceOasysEvent.CREATED, deleted = false),
      versionRow(version = 200, event = PersistenceOasysEvent.COUNTERSIGNED, deleted = true),
    )
    whenever(versionRepository.findNextPageIncludingDeleted(any(), any(), any()))
      .thenReturn(rows, emptyList())
    whenever(associationsService.findLatestAssociationDetailsByEntityIds(any()))
      .thenReturn(mapOf(entityUuid to EntityAssociationDetails("PK-123", "MDI", 5L)))

    job.replayAll()

    val captor = argumentCaptor<CoordinatorEvent>()
    verify(publisher, times(2)).publish(captor.capture())

    val first = captor.allValues[0]
    assertThat(first.eventType).isEqualTo(EventType.OASYS_VERSION_EVENT)
    assertThat(first.entityType).isEqualTo("AAP_PLAN")
    assertThat(first.entityUuid).isEqualTo(entityUuid)
    assertThat(first.occurredAt).isEqualTo(updatedAt)
    val firstPayload = first.message as VersionPayload
    assertThat(firstPayload.version).isEqualTo(100)
    assertThat(firstPayload.oasysEvent).isEqualTo(PersistenceOasysEvent.CREATED)
    assertThat(firstPayload.incrementedAt).isEqualTo(updatedAt)
    assertThat(firstPayload.deleted).isFalse()
    assertThat(firstPayload.association.oasysAssessmentPk).isEqualTo("PK-123")
    assertThat(firstPayload.association.regionPrisonCode).isEqualTo("MDI")
    assertThat(firstPayload.association.baseVersion).isEqualTo(5L)

    val secondPayload = captor.allValues[1].message as VersionPayload
    assertThat(secondPayload.version).isEqualTo(200)
    assertThat(secondPayload.oasysEvent).isEqualTo(PersistenceOasysEvent.COUNTERSIGNED)
    assertThat(secondPayload.deleted).isTrue()
  }

  @Test
  fun `replayAll uses updatedAt not createdAt for incrementedAt and occurredAt`() {
    val mutatedAt = LocalDateTime.of(2026, 6, 20, 8, 0, 0)
    val row = versionRow(version = 300, event = PersistenceOasysEvent.LOCKED, updated = mutatedAt)
    whenever(versionRepository.findNextPageIncludingDeleted(any(), any(), any()))
      .thenReturn(listOf(row), emptyList())
    whenever(associationsService.findLatestAssociationDetailsByEntityIds(any()))
      .thenReturn(mapOf(entityUuid to EntityAssociationDetails("PK-123", null, 1L)))

    job.replayAll()

    val captor = argumentCaptor<CoordinatorEvent>()
    verify(publisher).publish(captor.capture())
    val event = captor.firstValue
    assertThat(event.occurredAt).isEqualTo(mutatedAt)
    assertThat((event.message as VersionPayload).incrementedAt).isEqualTo(mutatedAt)
  }

  @Test
  fun `replayAll skips rows with no resolvable association PK`() {
    val orphanUuid = UUID.fromString("00000002-2222-2222-2222-000000000002")
    val rows = listOf(
      versionRow(version = 100, event = PersistenceOasysEvent.CREATED),
      versionRow(version = 100, event = PersistenceOasysEvent.CREATED, uuid = orphanUuid),
    )
    whenever(versionRepository.findNextPageIncludingDeleted(any(), any(), any()))
      .thenReturn(rows, emptyList())
    whenever(associationsService.findLatestAssociationDetailsByEntityIds(any()))
      .thenReturn(mapOf(entityUuid to EntityAssociationDetails("PK-123", "MDI", 5L)))
    // orphan has no live association and no fallback -> skipped
    whenever(associationsService.findAllOfAnyKindIncludingDeleted(orphanUuid)).thenReturn(emptyList())

    job.replayAll()

    val captor = argumentCaptor<CoordinatorEvent>()
    verify(publisher, times(1)).publish(captor.capture())
    assertThat(captor.firstValue.entityUuid).isEqualTo(entityUuid)
  }

  @Test
  fun `replayAll falls back to any-kind association when no live association exists`() {
    val row = versionRow(version = 100, event = PersistenceOasysEvent.CREATED)
    whenever(versionRepository.findNextPageIncludingDeleted(any(), any(), any()))
      .thenReturn(listOf(row), emptyList())
    whenever(associationsService.findLatestAssociationDetailsByEntityIds(any())).thenReturn(emptyMap())
    whenever(associationsService.findAllOfAnyKindIncludingDeleted(entityUuid)).thenReturn(
      listOf(
        OasysAssociation(
          createdAt = createdAt,
          entityUuid = entityUuid,
          oasysAssessmentPk = "PK-DELETED",
          regionPrisonCode = "LEI",
          baseVersion = 9L,
          deleted = true,
        ),
      ),
    )

    job.replayAll()

    val captor = argumentCaptor<CoordinatorEvent>()
    verify(publisher).publish(captor.capture())
    assertThat((captor.firstValue.message as VersionPayload).association.oasysAssessmentPk).isEqualTo("PK-DELETED")
  }

  @Test
  fun `replayAll walks every keyset page until exhausted`() {
    val page1 = listOf(versionRow(version = 100, event = PersistenceOasysEvent.CREATED))
    val page2 = listOf(versionRow(version = 200, event = PersistenceOasysEvent.LOCKED))
    whenever(versionRepository.findNextPageIncludingDeleted(any(), any(), any()))
      .thenReturn(page1, page2, emptyList())
    whenever(associationsService.findLatestAssociationDetailsByEntityIds(any()))
      .thenReturn(mapOf(entityUuid to EntityAssociationDetails("PK-123", "MDI", 5L)))

    job.replayAll()

    verify(versionRepository, times(3)).findNextPageIncludingDeleted(any(), any(), any())
    verify(publisher, times(2)).publish(any())
  }

  @Test
  fun `runWithAdvisoryLock skips the replay entirely when the lock is not acquired`() {
    val connection = mock<Connection>()
    val lockStatement = mock<PreparedStatement>()
    val lockResult = mock<ResultSet>()
    whenever(dataSource.connection).thenReturn(connection)
    whenever(connection.prepareStatement(eq("SELECT pg_try_advisory_lock(?)"))).thenReturn(lockStatement)
    whenever(lockStatement.executeQuery()).thenReturn(lockResult)
    whenever(lockResult.next()).thenReturn(true)
    whenever(lockResult.getBoolean(1)).thenReturn(false)

    job.runWithAdvisoryLock()

    verify(versionRepository, never()).findNextPageIncludingDeleted(any(), any(), any())
    verify(publisher, never()).publish(any())
    verify(connection, never()).prepareStatement(eq("SELECT pg_advisory_unlock(?)"))
  }

  @Test
  fun `runWithAdvisoryLock runs the replay and releases the lock when acquired`() {
    val connection = mock<Connection>()
    val lockStatement = mock<PreparedStatement>()
    val lockResult = mock<ResultSet>()
    val unlockStatement = mock<PreparedStatement>()
    val unlockResult = mock<ResultSet>()
    whenever(dataSource.connection).thenReturn(connection)
    whenever(connection.prepareStatement(eq("SELECT pg_try_advisory_lock(?)"))).thenReturn(lockStatement)
    whenever(connection.prepareStatement(eq("SELECT pg_advisory_unlock(?)"))).thenReturn(unlockStatement)
    whenever(lockStatement.executeQuery()).thenReturn(lockResult)
    whenever(lockResult.next()).thenReturn(true)
    whenever(lockResult.getBoolean(1)).thenReturn(true)
    whenever(unlockStatement.executeQuery()).thenReturn(unlockResult)
    whenever(unlockResult.next()).thenReturn(true)
    whenever(versionRepository.findNextPageIncludingDeleted(any(), any(), any())).thenReturn(emptyList())

    job.runWithAdvisoryLock()

    verify(versionRepository).findNextPageIncludingDeleted(any(), any(), any())
    verify(connection).prepareStatement(eq("SELECT pg_advisory_unlock(?)"))
  }
}

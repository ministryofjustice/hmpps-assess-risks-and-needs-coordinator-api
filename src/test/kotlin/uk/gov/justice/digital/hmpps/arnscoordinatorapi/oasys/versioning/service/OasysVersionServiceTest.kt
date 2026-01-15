package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionRepository
import java.time.LocalDateTime
import java.util.UUID

class OasysVersionServiceTest {

  @Mock
  private val repository: OasysVersionRepository = mock()

  private val clock: Clock = Mockito.mock()
  private val now = LocalDateTime.parse("2026-01-09T12:00:00")

  private lateinit var service: OasysVersionService

  @BeforeEach
  fun setup() {
    service = OasysVersionService(repository, clock)

    whenever(clock.now()).thenReturn(now)
  }

  private fun entity(
    entityUuid: UUID,
    version: Long,
    deleted: Boolean = false,
    createdBy: OasysEvent = OasysEvent.CREATED,
  ): OasysVersionEntity = OasysVersionEntity(
    entityUuid = entityUuid,
    version = version,
    createdBy = createdBy,
  ).apply {
    this.deleted = deleted
  }

  @Nested
  inner class CreateVersionFor {

    @Test
    fun `it creates version 0 when no previous version exists`() {
      val uuid = UUID.randomUUID()
      val event = OasysEvent.CREATED

      whenever(repository.findTopByEntityUuidOrderByVersionDesc(uuid)).thenReturn(null)
      // return the entity passed into save (common pattern)
      whenever(repository.save(any())).thenAnswer { it.arguments[0] as OasysVersionEntity }

      val saved = service.createVersionFor(event, uuid)

      assertEquals(uuid, saved.entityUuid)
      assertEquals(0L, saved.version)
      assertEquals(event, saved.createdBy)

      verify(repository).findTopByEntityUuidOrderByVersionDesc(uuid)
      verify(repository).save(
        check {
          assertEquals(uuid, it.entityUuid)
          assertEquals(0L, it.version)
          assertEquals(event, it.createdBy)
        },
      )
      verifyNoMoreInteractions(repository)
    }

    @Test
    fun `it increments the version when a previous version exists`() {
      val uuid = UUID.randomUUID()
      val event = OasysEvent.AWAITING_COUNTERSIGN
      val previous = entity(uuid, version = 41L)

      whenever(repository.findTopByEntityUuidOrderByVersionDesc(uuid)).thenReturn(previous)
      whenever(repository.save(any())).thenAnswer { it.arguments[0] as OasysVersionEntity }

      val saved = service.createVersionFor(event, uuid)

      assertEquals(42L, saved.version)
      verify(repository).save(check { assertEquals(42L, it.version) })
    }
  }

  @Nested
  inner class UpdateVersion {

    @Test
    fun `it throws when no previous version is found`() {
      val uuid = UUID.randomUUID()
      val event = OasysEvent.COUNTERSIGNED

      whenever(repository.findByEntityUuidAndVersion(uuid, 5L)).thenReturn(null)

      val ex = assertThrows<Error> {
        service.updateVersion(event, uuid, 5L)
      }

      assertEquals("Unable to update version, no previous version found for $uuid", ex.message)
      verify(repository).findByEntityUuidAndVersion(uuid, 5L)
      verify(repository, never()).saveAll(anyList())
    }

    @Test
    fun `it carries forward the createdBy timestamp and updates the updatedAt timestamp`() {
      val uuid = UUID.randomUUID()
      val event = OasysEvent.COUNTERSIGNED
      val existing = entity(uuid, version = 7L, createdBy = OasysEvent.AWAITING_COUNTERSIGN)

      whenever(repository.findByEntityUuidAndVersion(uuid, 7L)).thenReturn(existing)
      whenever(repository.save(any())).thenAnswer { it.arguments[0] as OasysVersionEntity }

      val saved = service.updateVersion(event, uuid, 7L)

      assertNotNull(saved)
      assertEquals(event, saved.createdBy)
      assertEquals(now, saved.updatedAt)

      verify(repository).save(
        check {
          assertEquals(event, it.createdBy)
          assertEquals(now, it.updatedAt)
        },
      )
    }
  }

  @Nested
  inner class SoftDeleteVersions {

    @Test
    fun `it throws when no versions are found in the provided range`() {
      val uuid = UUID.randomUUID()

      whenever(repository.findAllByEntityUuidAndVersionBetween(uuid, 1L, 3L)).thenReturn(emptyList())

      val ex = assertThrows<Error> {
        service.softDeleteVersions(uuid, from = 1L, to = 3L)
      }

      assertTrue(ex.message!!.contains("No versions found for entity $uuid between 1 to 3"))
      verify(repository).findAllByEntityUuidAndVersionBetween(uuid, 1L, 3L)
      verify(repository, never()).saveAll(anyList())
    }

    @Test
    fun `it uses the latest version when the 'to' value is null`() {
      val uuid = UUID.randomUUID()
      whenever(repository.findLatestVersionForEntityUuid(uuid)).thenReturn(10L)

      val found = listOf(entity(uuid, 8L), entity(uuid, 9L), entity(uuid, 10L))
      whenever(repository.findAllByEntityUuidAndVersionBetween(uuid, 8L, 10L)).thenReturn(found)
      whenever(repository.saveAll(anyList())).thenAnswer { it.arguments[0] as List<*> }

      val last = service.softDeleteVersions(uuid, from = 8L, to = null)

      verify(repository).findLatestVersionForEntityUuid(uuid)
      verify(repository).findAllByEntityUuidAndVersionBetween(uuid, 8L, 10L)

      verify(repository).saveAll(
        check<List<OasysVersionEntity>> {
          assertEquals(listOf(true, true, true), it.map { v -> v.deleted })
        },
      )

      assertEquals(10L, last.version)
      assertTrue(last.deleted)
    }

    @Test
    fun `it marks all returned entities deleted and returns the last version in the saveAll result`() {
      val uuid = UUID.randomUUID()
      val found = listOf(entity(uuid, 1L), entity(uuid, 2L), entity(uuid, 3L))
      whenever(repository.findAllByEntityUuidAndVersionBetween(uuid, 1L, 3L)).thenReturn(found)

      val savedList =
        listOf(entity(uuid, 1L, deleted = true), entity(uuid, 2L, deleted = true), entity(uuid, 3L, deleted = true))
      whenever(repository.saveAll(anyList())).thenReturn(savedList)

      val last = service.softDeleteVersions(uuid, 1L, 3L)

      assertEquals(3L, last.version)
      assertTrue(last.deleted)
    }
  }

  @Nested
  inner class UndeleteVersions {

    @Test
    fun `it throws when no deleted versions found in range`() {
      val uuid = UUID.randomUUID()
      whenever(repository.findAllDeletedByEntityUuidAndVersionBetween(uuid, 1L, 2L)).thenReturn(emptyList())

      assertThrows<Error> {
        service.undeleteVersions(uuid, 1L, 2L)
      }

      verify(repository).findAllDeletedByEntityUuidAndVersionBetween(uuid, 1L, 2L)
      verify(repository, never()).saveAll(anyList())
    }

    @Test
    fun `it uses the latest version when 'to' is null`() {
      val uuid = UUID.randomUUID()
      whenever(repository.findLatestVersionForEntityUuid(uuid)).thenReturn(4L)

      val found = listOf(entity(uuid, 3L, deleted = true), entity(uuid, 4L, deleted = true))
      whenever(repository.findAllDeletedByEntityUuidAndVersionBetween(uuid, 3L, 4L)).thenReturn(found)
      whenever(repository.saveAll(anyList())).thenAnswer { it.arguments[0] as List<OasysVersionEntity> }

      val last = service.undeleteVersions(uuid, from = 3L, to = null)

      verify(repository).findLatestVersionForEntityUuid(uuid)
      verify(repository).findAllDeletedByEntityUuidAndVersionBetween(uuid, 3L, 4L)

      verify(repository).saveAll(
        check<List<OasysVersionEntity>> {
          assertEquals(listOf(false, false), it.map { v -> v.deleted })
        },
      )

      assertEquals(4L, last.version)
      assertFalse(last.deleted)
    }

    @Test
    fun `marks all returned entities undeleted and returns last the last version in the saveAll result`() {
      val uuid = UUID.randomUUID()
      val found = listOf(entity(uuid, 1L, deleted = true), entity(uuid, 2L, deleted = true))
      whenever(repository.findAllDeletedByEntityUuidAndVersionBetween(uuid, 1L, 2L)).thenReturn(found)

      val savedList = listOf(entity(uuid, 1L, deleted = false), entity(uuid, 2L, deleted = false))
      whenever(repository.saveAll(anyList())).thenReturn(savedList)

      val last = service.undeleteVersions(uuid, 1L, 2L)

      assertEquals(2L, last.version)
      assertFalse(last.deleted)
    }
  }

  @Nested
  inner class FetchAllForEntityUuid {

    @Test
    fun `it fetches all entities matching the UUID`() {
      val uuid = UUID.randomUUID()
      val expected = listOf(entity(uuid, 0L), entity(uuid, 1L))

      whenever(repository.findAllByEntityUuid(uuid)).thenReturn(expected)

      val result = service.fetchAllForEntityUuid(uuid)

      assertSame(expected, result)
      verify(repository).findAllByEntityUuid(uuid)
      verifyNoMoreInteractions(repository)
    }
  }

  @Nested
  inner class GetLatestVersionNumberForEntityUuid {

    @Test
    fun `it gets the latest version number for a given entity UUID`() {
      val uuid = UUID.randomUUID()
      whenever(repository.findLatestVersionForEntityUuid(uuid)).thenReturn(123L)

      val result = service.getLatestVersionNumberForEntityUuid(uuid)

      assertEquals(123L, result)
      verify(repository).findLatestVersionForEntityUuid(uuid)
      verifyNoMoreInteractions(repository)
    }
  }
}

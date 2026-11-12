package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.AAPApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.PropertyValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook.PrepopulateFromOasys
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.MultiValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.SingleValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.Value
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.AssessmentResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UndeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.SanAssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.service.OasysVersionService
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.CreateAssessmentData as AAPCreateAssessmentData

class AAPStrengthsAndNeedsStrategyTest {
  private val aapApi: AAPApi = mock()
  private val oasysVersionService: OasysVersionService = mock()
  private val clock: Clock = mock()
  private val objectMapper = jacksonObjectMapper()
  private lateinit var strategy: AAPStrengthsAndNeedsStrategy

  private val now = LocalDateTime.parse("2026-01-09T12:00:00")

  @BeforeEach
  fun setup() {
    strategy = AAPStrengthsAndNeedsStrategy(
      aapApi = aapApi,
      oasysVersionService = oasysVersionService,
      clock = clock,
      objectMapper = objectMapper,
    )

    whenever(clock.now()).thenReturn(now)
  }

  @Nested
  inner class Create {

    private val versionedEntity = VersionedEntity(UUID.randomUUID(), 1, EntityType.AAP_SAN)

    @BeforeEach
    fun setup() {
      whenever(aapApi.createAssessment(any(), any())).thenReturn(AAPApi.ApiOperationResult.Success(versionedEntity))
      whenever(oasysVersionService.createVersionFor(OasysEvent.CREATED, versionedEntity.id))
        .thenReturn(OasysVersionEntity(createdBy = OasysEvent.CREATED, entityUuid = versionedEntity.id, version = versionedEntity.version))
    }

    private fun createdData(sanAssessmentType: SanAssessmentType?, oasysData: Map<String, Any?>?): AAPCreateAssessmentData {
      val captor = argumentCaptor<AAPCreateAssessmentData>()

      strategy.create(CreateData(assessment = CreateAssessmentData(UserDetails("id", "name"), sanAssessmentType, oasysData)))
      verify(aapApi).createAssessment(any(), captor.capture())

      return captor.firstValue
    }

    @Test
    fun `should send PrepopulateFromOasys hook when OASys data is supplied`() {
      val oasysData = mapOf("o3-4" to "M", "o8-1" to "NO")

      val data = createdData(SanAssessmentType.COMMUNITY, oasysData)

      assertThat(data.hooks.first()).isEqualTo(PrepopulateFromOasys(oasysData))
      assertThat(data.properties).isEqualTo(
        mapOf(
          "OASYS_CREATED" to PropertyValue(type = "Single", value = "2026-01-09T12:00:00Z"),
          "SAN_ASSESSMENT_TYPE" to PropertyValue(type = "Single", value = "COMMUNITY"),
        ),
      )
    }

    @Test
    fun `should not send PrepopulateFromOasys hook when OASys data is empty`() {
      val data = createdData(sanAssessmentType = null, oasysData = emptyMap())

      assertThat(data.hooks).noneMatch { it is PrepopulateFromOasys }
    }

    @Test
    fun `should not send PrepopulateFromOasys hook when SAN assessment type is CUSTODY`() {
      val data = createdData(SanAssessmentType.CUSTODY, mapOf("o3-4" to "2"))

      assertThat(data.hooks).noneMatch { it is PrepopulateFromOasys }
    }

    @Test
    fun `should send only the created timestamp and no PrepopulateFromOasys hook when SAN assessment type and OASys data are not supplied`() {
      val data = createdData(sanAssessmentType = null, oasysData = null)

      assertThat(data.properties).isEqualTo(
        mapOf("OASYS_CREATED" to PropertyValue(type = "Single", value = "2026-01-09T12:00:00Z")),
      )
      assertThat(data.hooks).noneMatch { it is PrepopulateFromOasys }
    }

    @Test
    fun `should truncate created timestamp to milliseconds when clock has sub-millisecond precision`() {
      whenever(clock.now()).thenReturn(LocalDateTime.parse("2026-01-09T12:00:00.123456789"))

      val properties = createdData(sanAssessmentType = null, oasysData = null).properties

      assertThat(properties["OASYS_CREATED"]?.value).isEqualTo("2026-01-09T12:00:00.123Z")
    }
  }

  @Nested
  inner class RecordOasysLink {

    private val entityUuid = UUID.randomUUID()
    private val userDetails = UserDetails("id", "name")
    private val user = AAPUser("id", "name")

    @Test
    fun `should send only the linked timestamp when SAN assessment type is not supplied`() {
      whenever(aapApi.updateAssessmentProperties(any(), any(), any()))
        .thenReturn(AAPApi.ApiOperationResult.Success(Unit))

      strategy.recordOasysLink(entityUuid, null, userDetails)

      verify(aapApi).updateAssessmentProperties(
        entityUuid,
        mapOf("OASYS_LINKED" to PropertyValue(type = "Single", value = "2026-01-09T12:00:00Z")),
        user,
      )
    }

    @Test
    fun `should send UTC linked timestamp and SAN assessment type without OASys answers when SAN is linked`() {
      val expectedProperties = mapOf(
        "OASYS_LINKED" to PropertyValue(type = "Single", value = "2026-01-09T12:00:00Z"),
        "SAN_ASSESSMENT_TYPE" to PropertyValue(type = "Single", value = "CUSTODY"),
      )
      whenever(aapApi.updateAssessmentProperties(entityUuid, expectedProperties, user))
        .thenReturn(AAPApi.ApiOperationResult.Success(Unit))

      val result = strategy.recordOasysLink(entityUuid, SanAssessmentType.CUSTODY, userDetails)

      assertThat(result).isEqualTo(OperationResult.Success(Unit))
      verify(aapApi).updateAssessmentProperties(entityUuid, expectedProperties, user)
    }

    @Test
    fun `should return failure when AAP properties cannot be updated`() {
      whenever(aapApi.updateAssessmentProperties(any(), any(), any()))
        .thenReturn(AAPApi.ApiOperationResult.Failure("Update failed"))

      val result = strategy.recordOasysLink(entityUuid, SanAssessmentType.CUSTODY, userDetails)

      assertThat(result).isEqualTo(OperationResult.Failure<Unit>("Update failed"))
    }
  }

  @Nested
  inner class Fetch {

    private fun queryResult(
      entityUuid: UUID,
      properties: Map<String, Value> = emptyMap(),
      answers: Map<String, Value> = emptyMap(),
    ) = AssessmentVersionQueryResult(
      assessmentUuid = entityUuid,
      aggregateUuid = UUID.randomUUID(),
      assessmentType = "STRENGTHS_AND_NEEDS",
      formVersion = "v1.0",
      createdAt = LocalDateTime.parse("2026-01-09T12:00:00"),
      updatedAt = LocalDateTime.parse("2026-01-09T12:30:00"),
      answers = answers,
      properties = properties,
      collections = emptyList(),
      collaborators = emptySet(),
      identifiers = emptyMap(),
    )

    @Test
    fun `maps the assessment response with metadata and answers`() {
      val entityUuid = UUID.randomUUID()
      val result = queryResult(
        entityUuid,
        answers = mapOf("q1" to SingleValue("a1"), "q2" to MultiValue(listOf("a2", "a3"))),
      )

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(AAPApi.ApiOperationResult.Success(result))

      val operationResult = strategy.fetch(entityUuid)

      assertThat(operationResult).isInstanceOf(OperationResult.Success::class.java)
      val response = (operationResult as OperationResult.Success<*>).data as AssessmentResponse
      assertThat(response.metaData.uuid).isEqualTo(entityUuid)
      assertThat(response.metaData.createdAt).isEqualTo(result.createdAt)
      assertThat(response.metaData.versionCreatedAt).isEqualTo(result.updatedAt)
      assertThat(response.metaData.versionUpdatedAt).isEqualTo(result.updatedAt)
      assertThat(response.metaData.versionNumber)
        .isEqualTo(result.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli())
      assertThat(response.metaData.formVersion).isEqualTo("v1.0")
      assertThat(response.assessment).isEqualTo(
        mapOf(
          "q1" to mapOf("value" to "a1"),
          "q2" to mapOf("values" to listOf("a2", "a3")),
        ),
      )
      verify(aapApi).fetchAssessment(entityUuid, now)
    }

    @Test
    fun `unescapes HTML entities in the answers`() {
      val entityUuid = UUID.randomUUID()
      // AAP HTML-escapes free text answers, e.g. "\" as "&#x5C;" and "/" as "&#x2F;"
      val result = queryResult(
        entityUuid,
        answers = mapOf("q1" to SingleValue("¯&#x5C;_(ツ)_&#x2F;¯")),
      )

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(AAPApi.ApiOperationResult.Success(result))

      val operationResult = strategy.fetch(entityUuid)

      val response = (operationResult as OperationResult.Success<*>).data as AssessmentResponse
      assertThat(response.assessment).isEqualTo(mapOf("q1" to mapOf("value" to "¯\\_(ツ)_/¯")))
    }

    @Test
    fun `oasysEquivalent is empty when the property is absent`() {
      val entityUuid = UUID.randomUUID()

      whenever(aapApi.fetchAssessment(entityUuid, now))
        .thenReturn(AAPApi.ApiOperationResult.Success(queryResult(entityUuid)))

      val operationResult = strategy.fetch(entityUuid)

      val response = (operationResult as OperationResult.Success<*>).data as AssessmentResponse
      assertThat(response.oasysEquivalent).isEmpty()
    }

    @Test
    fun `oasysEquivalent is parsed from the stringified JSON property`() {
      val entityUuid = UUID.randomUUID()
      val oasysEquivalentJson = """{"1.2":"YES","2.1":["A","B"],"nested":{"key":"value"}}"""

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Success(
          queryResult(entityUuid, properties = mapOf("oasys_equivalent" to SingleValue(oasysEquivalentJson))),
        ),
      )

      val operationResult = strategy.fetch(entityUuid)

      val response = (operationResult as OperationResult.Success<*>).data as AssessmentResponse
      assertThat(response.oasysEquivalent).isEqualTo(
        mapOf(
          "1.2" to "YES",
          "2.1" to listOf("A", "B"),
          "nested" to mapOf("key" to "value"),
        ),
      )
    }

    @Test
    fun `oasysEquivalent is empty when the property value is an empty JSON object`() {
      val entityUuid = UUID.randomUUID()

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Success(
          queryResult(entityUuid, properties = mapOf("oasys_equivalent" to SingleValue("{}"))),
        ),
      )

      val operationResult = strategy.fetch(entityUuid)

      val response = (operationResult as OperationResult.Success<*>).data as AssessmentResponse
      assertThat(response.oasysEquivalent).isEmpty()
    }

    @Test
    fun `returns failure when the oasys_equivalent property is not valid JSON`() {
      val entityUuid = UUID.randomUUID()

      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Success(
          queryResult(entityUuid, properties = mapOf("oasys_equivalent" to SingleValue("not-json"))),
        ),
      )

      val operationResult = strategy.fetch(entityUuid)

      assertThat(operationResult).isInstanceOf(OperationResult.Failure::class.java)
      assertThat((operationResult as OperationResult.Failure).errorMessage)
        .isEqualTo("Failed to fetch Strengths and Needs assessment for entity $entityUuid")
    }

    @Test
    fun `returns failure when the fetch call fails`() {
      val entityUuid = UUID.randomUUID()

      whenever(aapApi.fetchAssessment(entityUuid, now))
        .thenReturn(AAPApi.ApiOperationResult.Failure("Fetch error occurred"))

      val operationResult = strategy.fetch(entityUuid)

      assertThat(operationResult).isInstanceOf(OperationResult.Failure::class.java)
      assertThat((operationResult as OperationResult.Failure).errorMessage).isEqualTo("Fetch error occurred")
    }
  }

  @Nested
  inner class Undelete {
    val versionedEntity = OasysVersionEntity(
      createdBy = OasysEvent.LOCKED,
      entityUuid = UUID.randomUUID(),
      version = 2,
      deleted = true,
    )

    val expectedUser = AAPUser(id = "id", name = "name")

    @Nested
    inner class WithVersionTo {
      val undeleteData = UndeleteData(
        UserDetails("id", "name", UserType.OASYS),
        versionFrom = 1,
        versionTo = 2,
      )

      @Test
      fun `should undelete local versions only`() {
        whenever(oasysVersionService.undeleteVersions(versionedEntity.entityUuid, 1, 2)).thenReturn(versionedEntity)

        val result = strategy.undelete(undeleteData, versionedEntity.entityUuid)

        assertThat(result).isEqualTo(
          OperationResult.Success(
            VersionedEntity(
              id = versionedEntity.entityUuid,
              version = versionedEntity.version,
              entityType = EntityType.AAP_SAN,
            ),
          ),
        )
        verify(oasysVersionService).undeleteVersions(versionedEntity.entityUuid, 1, 2)
        verify(oasysVersionService, never()).findDeletedVersions(any(), any(), anyOrNull())
        verify(aapApi, never()).undeleteAssessment(any(), any(), any())
      }

      @Test
      fun `should return failure when local undelete fails`() {
        whenever(oasysVersionService.undeleteVersions(versionedEntity.entityUuid, 1, 2))
          .thenThrow(RuntimeException("No versions found"))

        val result = strategy.undelete(undeleteData, versionedEntity.entityUuid)

        assertThat(result).isEqualTo(
          OperationResult.Failure<VersionedEntity>("Something went wrong while un-deleting versions for entity ${versionedEntity.entityUuid}"),
        )
        verify(aapApi, never()).undeleteAssessment(any(), any(), any())
      }
    }

    @Nested
    inner class WithoutVersionTo {
      val undeleteData = UndeleteData(
        UserDetails("id", "name", UserType.OASYS),
        versionFrom = 1777301982983,
      )

      val expectedPointInTime: LocalDateTime = LocalDateTime.parse("2026-04-27T14:59:42.983")

      @Test
      fun `should undelete the AAP assessment from the base version and then the local versions`() {
        whenever(oasysVersionService.findDeletedVersions(versionedEntity.entityUuid, undeleteData.versionFrom, null))
          .thenReturn(listOf(versionedEntity))
        whenever(aapApi.undeleteAssessment(versionedEntity.entityUuid, expectedPointInTime, expectedUser))
          .thenReturn(AAPApi.ApiOperationResult.Success(Unit))
        whenever(oasysVersionService.undeleteVersions(versionedEntity.entityUuid, undeleteData.versionFrom, null))
          .thenReturn(versionedEntity)

        val result = strategy.undelete(undeleteData, versionedEntity.entityUuid)

        assertThat(result).isEqualTo(
          OperationResult.Success(
            VersionedEntity(
              id = versionedEntity.entityUuid,
              version = versionedEntity.version,
              entityType = EntityType.AAP_SAN,
            ),
          ),
        )
        inOrder(aapApi, oasysVersionService) {
          verify(oasysVersionService).findDeletedVersions(versionedEntity.entityUuid, undeleteData.versionFrom, null)
          verify(aapApi).undeleteAssessment(versionedEntity.entityUuid, expectedPointInTime, expectedUser)
          verify(oasysVersionService).undeleteVersions(versionedEntity.entityUuid, undeleteData.versionFrom, null)
        }
      }

      @Test
      fun `should return failure and leave local versions untouched when AAP undelete fails`() {
        whenever(oasysVersionService.findDeletedVersions(versionedEntity.entityUuid, undeleteData.versionFrom, null))
          .thenReturn(listOf(versionedEntity))
        whenever(aapApi.undeleteAssessment(versionedEntity.entityUuid, expectedPointInTime, expectedUser))
          .thenReturn(AAPApi.ApiOperationResult.Failure("AAP error"))

        val result = strategy.undelete(undeleteData, versionedEntity.entityUuid)

        assertThat(result).isEqualTo(OperationResult.Failure<VersionedEntity>("Failed to undelete AAP assessment: AAP error"))
        verify(oasysVersionService, never()).undeleteVersions(any(), any(), anyOrNull())
      }

      @Test
      fun `should preserve conflict status when AAP refuses an unsafe undelete`() {
        whenever(oasysVersionService.findDeletedVersions(versionedEntity.entityUuid, undeleteData.versionFrom, null))
          .thenReturn(listOf(versionedEntity))
        whenever(aapApi.undeleteAssessment(versionedEntity.entityUuid, expectedPointInTime, expectedUser))
          .thenReturn(AAPApi.ApiOperationResult.Failure("AAP conflict", statusCode = HttpStatus.CONFLICT))

        val result = strategy.undelete(undeleteData, versionedEntity.entityUuid)

        assertThat(result).isEqualTo(
          OperationResult.Failure<VersionedEntity>("Failed to undelete AAP assessment: AAP conflict", HttpStatus.CONFLICT),
        )
        verify(oasysVersionService, never()).undeleteVersions(any(), any(), anyOrNull())
      }

      @Test
      fun `should not call AAP when there are no local versions to restore`() {
        whenever(oasysVersionService.findDeletedVersions(versionedEntity.entityUuid, undeleteData.versionFrom, null))
          .thenReturn(emptyList())

        val result = strategy.undelete(undeleteData, versionedEntity.entityUuid)

        assertThat(result).isEqualTo(
          OperationResult.Failure<VersionedEntity>("No deleted versions found to undelete for entity ${versionedEntity.entityUuid}"),
        )
        verify(aapApi, never()).undeleteAssessment(any(), any(), any())
        verify(oasysVersionService, never()).undeleteVersions(any(), any(), anyOrNull())
      }
    }
  }

  @Nested
  inner class ChangePathway {
    private val entityUuid = UUID.randomUUID()
    private val userDetails = UserDetails("id", "name")
    private val user = AAPUser("id", "name")
    private val newVersion = OasysVersionEntity(createdBy = OasysEvent.CLONED, entityUuid = entityUuid, version = 123L)

    private fun assessmentOn(pathway: String?) = AssessmentVersionQueryResult(
      assessmentUuid = entityUuid,
      aggregateUuid = UUID.randomUUID(),
      assessmentType = "STRENGTHS_AND_NEEDS",
      formVersion = "v2.0",
      createdAt = now,
      updatedAt = now,
      answers = emptyMap(),
      properties = pathway?.let { mapOf("SAN_ASSESSMENT_TYPE" to SingleValue(it)) } ?: emptyMap(),
      collections = emptyList(),
      collaborators = emptySet(),
      identifiers = emptyMap(),
    )

    private fun givenStoredPathway(pathway: String?) {
      whenever(aapApi.fetchAssessment(entityUuid, now)).thenReturn(
        AAPApi.ApiOperationResult.Success(
          assessmentOn(
            pathway,
          ),
        ),
      )
    }

    @Test
    fun `should clear answers and return a new version when the pathway changes from community to custody`() {
      givenStoredPathway("COMMUNITY")
      whenever(aapApi.startNewPathway(entityUuid, user, SanAssessmentType.COMMUNITY, SanAssessmentType.CUSTODY))
        .thenReturn(AAPApi.ApiOperationResult.Success(Unit))
      whenever(oasysVersionService.createVersionFor(OasysEvent.CLONED, entityUuid)).thenReturn(newVersion)

      val result = strategy.changePathway(entityUuid, SanAssessmentType.CUSTODY, userDetails)

      assertThat(result).isEqualTo(OperationResult.Success(VersionedEntity(entityUuid, 123L, EntityType.AAP_SAN)))
      inOrder(aapApi, oasysVersionService) {
        verify(aapApi).fetchAssessment(entityUuid, now)
        verify(aapApi).startNewPathway(entityUuid, user, SanAssessmentType.COMMUNITY, SanAssessmentType.CUSTODY)
        verify(oasysVersionService).createVersionFor(OasysEvent.CLONED, entityUuid)
      }
    }

    @Test
    fun `should clear answers and return a new version when the pathway changes from custody to community`() {
      givenStoredPathway("CUSTODY")
      whenever(aapApi.startNewPathway(entityUuid, user, SanAssessmentType.CUSTODY, SanAssessmentType.COMMUNITY))
        .thenReturn(AAPApi.ApiOperationResult.Success(Unit))
      whenever(oasysVersionService.createVersionFor(OasysEvent.CLONED, entityUuid)).thenReturn(newVersion)

      val result = strategy.changePathway(entityUuid, SanAssessmentType.COMMUNITY, userDetails)

      assertThat(result).isEqualTo(OperationResult.Success(VersionedEntity(entityUuid, 123L, EntityType.AAP_SAN)))
      inOrder(aapApi, oasysVersionService) {
        verify(aapApi).fetchAssessment(entityUuid, now)
        verify(aapApi).startNewPathway(entityUuid, user, SanAssessmentType.CUSTODY, SanAssessmentType.COMMUNITY)
        verify(oasysVersionService).createVersionFor(OasysEvent.CLONED, entityUuid)
      }
    }

    @Test
    fun `should treat a missing pathway as community and clear answers when moving to custody`() {
      givenStoredPathway(null)
      whenever(aapApi.startNewPathway(entityUuid, user, SanAssessmentType.COMMUNITY, SanAssessmentType.CUSTODY))
        .thenReturn(AAPApi.ApiOperationResult.Success(Unit))
      whenever(oasysVersionService.createVersionFor(OasysEvent.CLONED, entityUuid)).thenReturn(newVersion)

      val result = strategy.changePathway(entityUuid, SanAssessmentType.CUSTODY, userDetails)

      assertThat(result).isEqualTo(OperationResult.Success(VersionedEntity(entityUuid, 123L, EntityType.AAP_SAN)))
      verify(aapApi).startNewPathway(entityUuid, user, SanAssessmentType.COMMUNITY, SanAssessmentType.CUSTODY)
    }

    @Test
    fun `should do nothing when the pathway does not change`() {
      givenStoredPathway("COMMUNITY")

      val result = strategy.changePathway(entityUuid, SanAssessmentType.COMMUNITY, userDetails)

      assertThat(result).isEqualTo(OperationResult.Success(null))
      verify(aapApi).fetchAssessment(entityUuid, now)
      verify(aapApi, never()).startNewPathway(any(), any(), any(), any())
      verify(oasysVersionService, never()).createVersionFor(any(), any())
    }

    @Test
    fun `should return failure and not create a version when the clear fails`() {
      whenever(
        aapApi.fetchAssessment(
          entityUuid,
          now,
        ),
      ).thenReturn(AAPApi.ApiOperationResult.Failure("Fetch failed"))

      val result = strategy.changePathway(entityUuid, SanAssessmentType.COMMUNITY, userDetails)

      assertThat(result).isInstanceOf(OperationResult.Failure::class.java)
      verify(aapApi, never()).startNewPathway(any(), any(), any(), any())
    }

    @Test
    fun `should return clear failed when the startNewPathway call fails`() {
      givenStoredPathway("COMMUNITY")
      whenever(aapApi.startNewPathway(entityUuid, user, SanAssessmentType.COMMUNITY, SanAssessmentType.CUSTODY))
        .thenReturn(AAPApi.ApiOperationResult.Failure("Start new pathway failed"))

      val result = strategy.changePathway(entityUuid, SanAssessmentType.CUSTODY, userDetails)

      assertThat(result).isInstanceOf(OperationResult.Failure::class.java)
      verify(oasysVersionService, never()).createVersionFor(any(), any())
    }
  }
}

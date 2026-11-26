package uk.gov.justice.digital.hmpps.arnscoordinatorapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysMergeRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysTransferAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.SignType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.dto.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.dto.SubjectDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.CoordinatorEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.DeleteFlagUpdatePayload
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.OasysEventFactory
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.OasysEventPublisher
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.VersionPayload
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.CoordinatorException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.NoAssociationsException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.NoAssociationsForEntityException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.NoAssociationsForEntityUuidException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.NoDeletedAssociationsException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.AAPApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.Command
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.PropertyValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.SoftDeleteAssessmentCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.Timeline
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.UndeleteAssessmentCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.UpdateFlagsCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.DailyVersionsQuery
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.Query
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.TimelineQuery
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command.CommandResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command.CommandSuccessResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command.CommandsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.DailyVersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.DailyVersionsQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.PageInfo
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.QueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.SingleValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.TimelineItem
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.TimelineQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.AAPAssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.VersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.plan.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysAssociationEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysVersionEntity
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.stream.Stream

class OasysCoordinatorServiceTest {
  private val now = LocalDateTime.parse("2026-01-09T12:00:00")
  private val clock: Clock = mock { on { now() } doAnswer { now } }

  private val aapApi: AAPApi = mock()
  private val oasysAssociationsService: OasysAssociationsService = mock()
  private val oasysVersionService: OasysVersionService = mock()

  private val publishedEvents = mutableListOf<CoordinatorEvent>()

  private val service = OasysCoordinatorService(
    sentencePlanService = SentencePlanService(),
    strengthsAndNeedsService = StrengthsAndNeedsService(jacksonObjectMapper()),
    oasysAssociationsService = oasysAssociationsService,
    oasysVersionService = oasysVersionService,
    oasysEventPublisher = OasysEventPublisher { publishedEvents.add(it) },
    oasysEventFactory = OasysEventFactory(clock),
    aapApi = aapApi,
    clock = clock,
  )

  private val userDetails = OasysUserDetails(id = "user-id", name = "User Name")
  private val aapUser = AAPUser(id = "user-id", name = "User Name")
  private val coordinatorUser = AAPUser(id = "COORDINATOR_API", name = "Coordinator API User")

  private val oasysAssessmentPk = "1001"
  private val planUuid: UUID = UUID.randomUUID()
  private val sanUuid: UUID = UUID.randomUUID()

  // Fake AAP: create commands are answered with the UUID registered for their assessment type, and queries with
  // whatever the test registered for them. Each answer lines up with the request it came from, as AAP does.
  private val createdUuids = mutableMapOf<String, UUID>()
  private val queryResults = mutableListOf<(Query) -> QueryResult?>()

  private var nextVersion = 1_000_000L
  private val createdVersions = mutableListOf<OasysVersionEntity>()

  @BeforeEach
  fun setup() {
    createdUuids[AAPAssessmentType.SENTENCE_PLAN.name] = planUuid
    createdUuids[AAPAssessmentType.STRENGTHS_AND_NEEDS.name] = sanUuid

    whenever(aapApi.dispatchCommands(any())).doAnswer { invocation ->
      CommandsResponse(
        invocation.getArgument<List<Command>>(0).map { command ->
          CommandResponse(
            request = command,
            result = when (command) {
              is CreateAssessmentCommand -> CreateAssessmentCommandResult(assessmentUuid = createdUuids.getValue(command.assessmentType))
              else -> CommandSuccessResult()
            },
          )
        },
      )
    }

    whenever(aapApi.dispatchQueries(any<List<Pair<Query, (QueryResult) -> Unit>>>())).doAnswer { invocation ->
      invocation.getArgument<List<Pair<Query, (QueryResult) -> Unit>>>(0).forEach { (query, callback) ->
        callback(queryResults.firstNotNullOfOrNull { it(query) } ?: error("No AAP query result registered for $query"))
      }
    }

    whenever(oasysAssociationsService.save(any())).doAnswer { it.getArgument(0) }
    whenever(oasysAssociationsService.getExistingAssociationTypes(any())).thenReturn(emptyList())

    whenever(oasysVersionService.createVersionFor(any(), any())).doAnswer { invocation ->
      OasysVersionEntity(createdBy = invocation.getArgument(0), version = nextVersion++, entityUuid = invocation.getArgument(1))
        .also(createdVersions::add)
    }
    whenever(oasysVersionService.updateVersion(any(), any(), any())).doAnswer { invocation ->
      OasysVersionEntity(createdBy = invocation.getArgument(0), entityUuid = invocation.getArgument(1), version = invocation.getArgument(2))
    }
    whenever(oasysVersionService.softDeleteVersions(any(), any(), anyOrNull())).doAnswer { invocation ->
      OasysVersionEntity(createdBy = OasysEvent.CREATED, entityUuid = invocation.getArgument(0), version = invocation.getArgument(1), deleted = true)
    }
    whenever(oasysVersionService.undeleteVersions(any(), any(), anyOrNull())).doAnswer { invocation ->
      OasysVersionEntity(createdBy = OasysEvent.CREATED, entityUuid = invocation.getArgument(0), version = invocation.getArgument(1))
    }
  }

  private fun association(
    entityType: EntityType,
    entityUuid: UUID = if (entityType == EntityType.AAP_PLAN) planUuid else sanUuid,
    oasysAssessmentPk: String = this.oasysAssessmentPk,
    baseVersion: Long = 0,
    createdAt: LocalDateTime = now.minusDays(1),
    deleted: Boolean = false,
  ) = OasysAssociationEntity(
    id = 1,
    entityType = entityType,
    entityUuid = entityUuid,
    oasysAssessmentPk = oasysAssessmentPk,
    baseVersion = baseVersion,
    createdAt = createdAt,
    regionPrisonCode = "MDI",
    deleted = deleted,
  )

  private fun givenAssociations(vararg associations: OasysAssociationEntity) {
    whenever(oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)).thenReturn(associations.toList())
  }

  private fun dispatchedCommands(): List<Command> = argumentCaptor<List<Command>>()
    .apply { verify(aapApi).dispatchCommands(capture()) }
    .firstValue

  private fun dispatchedQueries(): List<Query> = argumentCaptor<List<Pair<Query, (QueryResult) -> Unit>>>()
    .apply { verify(aapApi).dispatchQueries(capture()) }
    .firstValue
    .map { it.first }

  private fun versionPayloads() = publishedEvents.map { it.message as VersionPayload }

  @Nested
  inner class Create {
    private fun request(
      assessmentType: AssessmentType = AssessmentType.SAN_SP,
      previousOasysSanPk: String? = null,
      previousOasysSpPk: String? = null,
      newPeriodOfSupervision: String? = null,
    ) = OasysCreateRequest(
      oasysAssessmentPk = oasysAssessmentPk,
      previousOasysSanPk = previousOasysSanPk,
      previousOasysSpPk = previousOasysSpPk,
      regionPrisonCode = "MDI",
      planType = PlanType.INITIAL,
      assessmentType = assessmentType,
      userDetails = userDetails,
      newPeriodOfSupervision = newPeriodOfSupervision,
    )

    private fun savedAssociations(): List<OasysAssociationEntity> = argumentCaptor<OasysAssociationEntity>()
      .apply { verify(oasysAssociationsService, atLeast(0)).save(capture()) }
      .allValues

    private fun givenPreviousAssociation(entityType: EntityType, previousPk: String, entityUuid: UUID) {
      whenever(oasysAssociationsService.findAssociationsByPkAndType(previousPk, entityType))
        .thenReturn(listOf(association(entityType, entityUuid = entityUuid, oasysAssessmentPk = previousPk)))
    }

    @Test
    fun `throws a conflict when the PK already has associations`() {
      whenever(oasysAssociationsService.getExistingAssociationTypes(oasysAssessmentPk)).thenReturn(listOf(EntityType.AAP_SAN))

      val exception = assertThrows<ConflictException> { service.create(request()) }

      assertThat(exception.message).isEqualTo("Cannot create due to conflicting associations: AAP_SAN")
      verify(aapApi, never()).dispatchCommands(any())
      assertThat(publishedEvents).isEmpty()
    }

    @Test
    fun `creates a new SP and SAN when there are no previous PKs`() {
      val response = service.create(request())

      val commands = dispatchedCommands()
      assertThat(commands).hasSize(2)
      assertThat(commands.map { (it as CreateAssessmentCommand).assessmentType })
        .containsExactly(AAPAssessmentType.SENTENCE_PLAN.name, AAPAssessmentType.STRENGTHS_AND_NEEDS.name)
      assertThat(commands.map { it.user }).containsOnly(aapUser)
      commands.forEach { assertThat((it as CreateAssessmentCommand).flags).containsExactly("SAN_BETA") }

      assertThat(savedAssociations().map { Triple(it.entityType, it.entityUuid, it.oasysAssessmentPk) }).containsExactlyInAnyOrder(
        Triple(EntityType.AAP_PLAN, planUuid, oasysAssessmentPk),
        Triple(EntityType.AAP_SAN, sanUuid, oasysAssessmentPk),
      )
      assertThat(savedAssociations().map { it.regionPrisonCode }).containsOnly("MDI")

      val planVersion = createdVersions.single { it.entityUuid == planUuid }
      val sanVersion = createdVersions.single { it.entityUuid == sanUuid }
      assertThat(createdVersions.map { it.createdBy }).containsOnly(OasysEvent.CREATED)
      assertThat(response.sentencePlanId).isEqualTo(planUuid)
      assertThat(response.sentencePlanVersion).isEqualTo(planVersion.version)
      assertThat(response.sanAssessmentId).isEqualTo(sanUuid)
      assertThat(response.sanAssessmentVersion).isEqualTo(sanVersion.version)

      assertThat(publishedEvents).hasSize(1)
      assertThat(publishedEvents.single().entityUuid).isEqualTo(planUuid)
      assertThat(versionPayloads().single().oasysEvent).isEqualTo(OasysEvent.CREATED)
    }

    @Test
    fun `builds the SP create command with the plan type and subject identifiers`() {
      service.create(request().copy(subjectDetails = SubjectDetails(crn = "X123456", nomisId = "A1234BC")))

      val planCommand = dispatchedCommands().filterIsInstance<CreateAssessmentCommand>().first()
      assertThat(planCommand.formVersion).isEqualTo("v1.0")
      assertThat(planCommand.properties).isEqualTo(mapOf("PLAN_TYPE" to PropertyValue(type = "Single", value = "INITIAL")))
      assertThat(planCommand.identifiers?.mapKeys { it.key.name }).isEqualTo(mapOf("CRN" to "X123456", "NOMIS_ID" to "A1234BC"))
    }

    @Test
    fun `only creates a sentence plan for an SP assessment`() {
      val response = service.create(request(assessmentType = AssessmentType.SP))

      val commands = dispatchedCommands()
      assertThat(commands).hasSize(1)
      assertThat((commands.single() as CreateAssessmentCommand).assessmentType).isEqualTo(AAPAssessmentType.SENTENCE_PLAN.name)
      assertThat((commands.single() as CreateAssessmentCommand).flags).isEmpty()
      assertThat(savedAssociations().map { it.entityType }).containsExactly(EntityType.AAP_PLAN)
      assertThat(response.sentencePlanId).isEqualTo(planUuid)
      assertThat(response.sanAssessmentId).isEqualTo(UUID(0, 0))
    }

    @Test
    fun `ignores a previous SAN PK for an SP assessment`() {
      service.create(request(assessmentType = AssessmentType.SP, previousOasysSanPk = "999"))

      verify(oasysAssociationsService, never()).findAssociationsByPkAndType("999", EntityType.AAP_SAN)
      assertThat(savedAssociations().map { it.entityType }).containsExactly(EntityType.AAP_PLAN)
    }

    @Test
    fun `links an existing SP and creates a new SAN`() {
      val existingPlanUuid = UUID.randomUUID()
      givenPreviousAssociation(EntityType.AAP_PLAN, "999", existingPlanUuid)

      val response = service.create(request(previousOasysSpPk = "999"))

      val commands = dispatchedCommands()
      assertThat(commands.filterIsInstance<CreateAssessmentCommand>().map { it.assessmentType })
        .containsExactly(AAPAssessmentType.STRENGTHS_AND_NEEDS.name)
      assertThat(commands.filterIsInstance<UpdateFlagsCommand>().single())
        .isEqualTo(
          UpdateFlagsCommand(
            assessmentUuid = existingPlanUuid,
            user = aapUser,
            flags = listOf("SAN_BETA"),
            timeline = Timeline(type = "FLAGS_UPDATED"),
          ),
        )

      val clonedPlanVersion = createdVersions.single { it.entityUuid == existingPlanUuid }
      assertThat(clonedPlanVersion.createdBy).isEqualTo(OasysEvent.CLONED)

      val savedPlan = savedAssociations().single { it.entityType == EntityType.AAP_PLAN }
      assertThat(savedPlan.entityUuid).isEqualTo(existingPlanUuid)
      assertThat(savedPlan.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
      assertThat(savedPlan.baseVersion).isEqualTo(clonedPlanVersion.version)
      assertThat(savedAssociations().single { it.entityType == EntityType.AAP_SAN }.entityUuid).isEqualTo(sanUuid)

      assertThat(response.sentencePlanId).isEqualTo(existingPlanUuid)
      assertThat(response.sanAssessmentId).isEqualTo(sanUuid)
      assertThat(versionPayloads().single().oasysEvent).isEqualTo(OasysEvent.CLONED)
    }

    @Test
    fun `links an existing SAN and creates a new SP`() {
      val existingSanUuid = UUID.randomUUID()
      givenPreviousAssociation(EntityType.AAP_SAN, "999", existingSanUuid)

      val response = service.create(request(previousOasysSanPk = "999"))

      val commands = dispatchedCommands()
      assertThat(commands.filterIsInstance<CreateAssessmentCommand>().map { it.assessmentType })
        .containsExactly(AAPAssessmentType.SENTENCE_PLAN.name)
      assertThat(commands.filterIsInstance<UpdateFlagsCommand>().single().assessmentUuid).isEqualTo(existingSanUuid)
      assertThat(createdVersions.single { it.entityUuid == existingSanUuid }.createdBy).isEqualTo(OasysEvent.CLONED)
      assertThat(response.sentencePlanId).isEqualTo(planUuid)
      assertThat(response.sanAssessmentId).isEqualTo(existingSanUuid)
    }

    @Test
    fun `links both existing entities when both previous PKs are supplied`() {
      val existingPlanUuid = UUID.randomUUID()
      val existingSanUuid = UUID.randomUUID()
      givenPreviousAssociation(EntityType.AAP_PLAN, "998", existingPlanUuid)
      givenPreviousAssociation(EntityType.AAP_SAN, "999", existingSanUuid)

      val response = service.create(request(previousOasysSpPk = "998", previousOasysSanPk = "999"))

      val commands = dispatchedCommands()
      assertThat(commands).allMatch { it is UpdateFlagsCommand }
      assertThat(commands.map { (it as UpdateFlagsCommand).assessmentUuid }).containsExactly(existingPlanUuid, existingSanUuid)
      assertThat(createdVersions.map { it.createdBy }).containsOnly(OasysEvent.CLONED)
      assertThat(response.sentencePlanId).isEqualTo(existingPlanUuid)
      assertThat(response.sanAssessmentId).isEqualTo(existingSanUuid)
    }

    @Test
    fun `clears the AAP flags when an SP assessment links an existing sentence plan`() {
      val existingPlanUuid = UUID.randomUUID()
      givenPreviousAssociation(EntityType.AAP_PLAN, "999", existingPlanUuid)

      service.create(request(assessmentType = AssessmentType.SP, previousOasysSpPk = "999"))

      assertThat(dispatchedCommands().single()).isInstanceOf(UpdateFlagsCommand::class.java)
      assertThat((dispatchedCommands().single() as UpdateFlagsCommand).flags).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(EntityType::class)
    fun `throws not found when a previous PK has no association`(entityType: EntityType) {
      whenever(oasysAssociationsService.findAssociationsByPkAndType("999", entityType)).thenReturn(emptyList())

      val exception = assertThrows<NoAssociationsForEntityException> {
        service.create(
          when (entityType) {
            EntityType.AAP_PLAN -> request(previousOasysSpPk = "999")
            EntityType.AAP_SAN -> request(previousOasysSanPk = "999")
          },
        )
      }

      assertThat(exception.message).isEqualTo("No $entityType association found for PK 999")
      verify(aapApi, never()).dispatchCommands(any())
      verify(oasysAssociationsService, never()).save(any())
      assertThat(publishedEvents).isEmpty()
    }

    @Test
    fun `resets an existing sentence plan for a new period of supervision`() {
      val existingPlanUuid = UUID.randomUUID()
      givenPreviousAssociation(EntityType.AAP_PLAN, "999", existingPlanUuid)

      val response = service.create(request(assessmentType = AssessmentType.SP, previousOasysSpPk = "999", newPeriodOfSupervision = "Y"))

      val commands = dispatchedCommands()
      assertThat(commands.filterIsInstance<UpdateAssessmentPropertiesCommand>().single().assessmentUuid).isEqualTo(existingPlanUuid)
      assertThat(commands.filterIsInstance<UpdateFlagsCommand>().single().assessmentUuid).isEqualTo(existingPlanUuid)

      val planVersion = createdVersions.single()
      assertThat(planVersion.createdBy).isEqualTo(OasysEvent.CREATED)
      assertThat(response.sentencePlanVersion).isEqualTo(planVersion.version)
      assertThat(versionPayloads().single().oasysEvent).isEqualTo(OasysEvent.CREATED)
    }

    @Test
    fun `does not reset the plan when it is not a new period of supervision`() {
      givenPreviousAssociation(EntityType.AAP_PLAN, "999", UUID.randomUUID())

      service.create(request(assessmentType = AssessmentType.SP, previousOasysSpPk = "999", newPeriodOfSupervision = "N"))

      assertThat(dispatchedCommands().filterIsInstance<UpdateAssessmentPropertiesCommand>()).isEmpty()
      assertThat(createdVersions.single().createdBy).isEqualTo(OasysEvent.CLONED)
    }

    @Test
    fun `creates a new sentence plan for a new period of supervision when there is no previous SP`() {
      val response = service.create(request(assessmentType = AssessmentType.SP, newPeriodOfSupervision = "Y"))

      assertThat((dispatchedCommands().single() as CreateAssessmentCommand).assessmentType).isEqualTo(AAPAssessmentType.SENTENCE_PLAN.name)
      assertThat(response.sentencePlanId).isEqualTo(planUuid)
    }

    @Test
    fun `does not store associations or publish an event when AAP fails`() {
      givenPreviousAssociation(EntityType.AAP_PLAN, "999", UUID.randomUUID())
      whenever(aapApi.dispatchCommands(any())).thenThrow(CoordinatorException("AAP is down"))

      assertThrows<CoordinatorException> { service.create(request(previousOasysSpPk = "999")) }

      verify(oasysAssociationsService, never()).save(any())
      verify(oasysVersionService, never()).createVersionFor(any(), any())
      assertThat(publishedEvents).isEmpty()
    }
  }

  @Nested
  inner class Get {
    private fun versionResult(entityUuid: UUID, assessmentType: AAPAssessmentType, properties: Map<String, SingleValue> = emptyMap()) = AssessmentVersionQueryResult(
      assessmentUuid = entityUuid,
      aggregateUuid = UUID.randomUUID(),
      assessmentType = assessmentType.name,
      formVersion = "v1.0",
      createdAt = now.minusDays(1),
      updatedAt = now.minusHours(1),
      answers = mapOf("q1" to SingleValue("a1")),
      properties = properties,
      collections = emptyList(),
      collaborators = emptySet(),
      identifiers = emptyMap(),
    )

    @BeforeEach
    fun registerQueryResults() {
      queryResults.add { query ->
        when (query.assessmentIdentifier.uuid) {
          planUuid -> versionResult(planUuid, AAPAssessmentType.SENTENCE_PLAN, mapOf("PLAN_TYPE" to SingleValue("REVIEW")))
          sanUuid -> versionResult(sanUuid, AAPAssessmentType.STRENGTHS_AND_NEEDS)
          else -> null
        }
      }
    }

    private val expectedVersion = now.minusHours(1).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `throws not found when there are no associations`() {
      givenAssociations()

      assertThrows<NoAssociationsException> { service.get(oasysAssessmentPk) }
      verify(aapApi, never()).dispatchQueries(any<List<Pair<Query, (QueryResult) -> Unit>>>())
    }

    @Test
    fun `returns the latest version of the plan and SAN`() {
      givenAssociations(association(EntityType.AAP_PLAN), association(EntityType.AAP_SAN))

      val response = service.get(oasysAssessmentPk)

      assertThat(dispatchedQueries()).containsExactly(
        AssessmentVersionQuery(user = coordinatorUser, assessmentIdentifier = AssessmentIdentifier(planUuid), timestamp = now),
        AssessmentVersionQuery(user = coordinatorUser, assessmentIdentifier = AssessmentIdentifier(sanUuid), timestamp = now),
      )
      assertThat(response.sentencePlanId).isEqualTo(planUuid)
      assertThat(response.sentencePlanVersion).isEqualTo(expectedVersion)
      assertThat(response.planType).isEqualTo(PlanType.REVIEW)
      assertThat(response.sanAssessmentId).isEqualTo(sanUuid)
      assertThat(response.sanAssessmentVersion).isEqualTo(expectedVersion)
      assertThat(response.sanAssessmentData).isEqualTo(mapOf("q1" to mapOf("value" to "a1")))
    }

    @Test
    fun `returns the plan when it is the only association`() {
      givenAssociations(association(EntityType.AAP_PLAN))

      val response = service.get(oasysAssessmentPk)

      assertThat(response.sentencePlanId).isEqualTo(planUuid)
      assertThat(response.sanAssessmentId).isEqualTo(UUID(0, 0))
    }

    @Test
    fun `returns the SAN when it is the only association`() {
      givenAssociations(association(EntityType.AAP_SAN))

      val response = service.get(oasysAssessmentPk)

      assertThat(response.sanAssessmentId).isEqualTo(sanUuid)
      assertThat(response.sentencePlanId).isEqualTo(UUID(0, 0))
    }
  }

  @Nested
  inner class GetByEntityId {
    @BeforeEach
    fun registerQueryResults() {
      queryResults.add { query ->
        AssessmentVersionQueryResult(
          assessmentUuid = query.assessmentIdentifier.uuid,
          aggregateUuid = UUID.randomUUID(),
          assessmentType = "ANY",
          formVersion = "v1.0",
          createdAt = now,
          updatedAt = now,
          answers = emptyMap(),
          properties = mapOf("PLAN_TYPE" to SingleValue("INITIAL")),
          collections = emptyList(),
          collaborators = emptySet(),
          identifiers = emptyMap(),
        )
      }
    }

    @Test
    fun `throws not found when the entity has no associations`() {
      whenever(oasysAssociationsService.findOasysPkByEntityId(planUuid)).thenReturn(null)

      assertThrows<NoAssociationsForEntityUuidException> { service.getByEntityId(planUuid, EntityType.AAP_PLAN) }
    }

    @Test
    fun `throws not found when the PK has no association of the requested type`() {
      whenever(oasysAssociationsService.findOasysPkByEntityId(planUuid)).thenReturn(oasysAssessmentPk)
      givenAssociations(association(EntityType.AAP_PLAN))

      assertThrows<NoAssociationsForEntityException> { service.getByEntityId(planUuid, EntityType.AAP_SAN) }
    }

    @ParameterizedTest
    @EnumSource(EntityType::class)
    fun `only fetches the requested entity type`(entityType: EntityType) {
      whenever(oasysAssociationsService.findOasysPkByEntityId(planUuid)).thenReturn(oasysAssessmentPk)
      givenAssociations(association(EntityType.AAP_PLAN), association(EntityType.AAP_SAN))

      val response = service.getByEntityId(planUuid, entityType)

      val expectedUuid = if (entityType == EntityType.AAP_PLAN) planUuid else sanUuid
      assertThat(dispatchedQueries().map { it.assessmentIdentifier.uuid }).containsExactly(expectedUuid)
      assertThat(response.sentencePlanId).isEqualTo(if (entityType == EntityType.AAP_PLAN) planUuid else UUID(0, 0))
      assertThat(response.sanAssessmentId).isEqualTo(if (entityType == EntityType.AAP_SAN) sanUuid else UUID(0, 0))
    }
  }

  @Nested
  inner class GetVersionsByEntityId {
    private val day1 = LocalDateTime.parse("2025-06-01T10:00:00")
    private val day2 = LocalDateTime.parse("2025-06-02T10:00:00")
    private val day3 = LocalDateTime.parse("2025-06-03T10:00:00")

    private fun epoch(dateTime: LocalDateTime) = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `throws not found when the entity has no associations`() {
      whenever(oasysAssociationsService.findOasysPkByEntityId(planUuid)).thenReturn(null)

      assertThrows<NoAssociationsForEntityUuidException> { service.getVersionsByEntityId(planUuid) }
    }

    @Test
    fun `merges local and AAP versions of each associated entity`() {
      whenever(oasysAssociationsService.findOasysPkByEntityId(sanUuid)).thenReturn(oasysAssessmentPk)
      givenAssociations(association(EntityType.AAP_PLAN), association(EntityType.AAP_SAN))

      val lockedPlanVersion = OasysVersionEntity(createdBy = OasysEvent.LOCKED, version = epoch(day1), entityUuid = planUuid, createdAt = day1, updatedAt = day1)
      whenever(oasysVersionService.fetchAllForEntityUuid(planUuid)).thenReturn(listOf(lockedPlanVersion))
      whenever(oasysVersionService.fetchAllForEntityUuid(sanUuid)).thenReturn(emptyList())

      val planDailyUuid = UUID.randomUUID()
      val agreedTimelineUuid = UUID.randomUUID()
      val sanDailyUuid = UUID.randomUUID()
      queryResults.add { query ->
        when {
          query is DailyVersionsQuery && query.assessmentIdentifier.uuid == planUuid -> DailyVersionsQueryResult(
            listOf(DailyVersionDetails(createdAt = day1, updatedAt = day1, lastTimelineItemUuid = planDailyUuid)),
          )
          query is TimelineQuery && query.assessmentIdentifier.uuid == planUuid -> TimelineQueryResult(
            timeline = listOf(
              TimelineItem(uuid = agreedTimelineUuid, timestamp = day2, user = aapUser, assessment = planUuid, event = null, customType = "PLAN_AGREEMENT_STATUS_CHANGED", customData = mapOf("status" to "AGREED")),
              TimelineItem(uuid = UUID.randomUUID(), timestamp = day3, user = aapUser, assessment = planUuid, event = null, customType = "PLAN_AGREEMENT_STATUS_CHANGED", customData = mapOf("status" to "DRAFT")),
            ),
            pageInfo = PageInfo(pageNumber = 0, totalPages = 1),
          )
          query is DailyVersionsQuery && query.assessmentIdentifier.uuid == sanUuid -> DailyVersionsQueryResult(
            listOf(DailyVersionDetails(createdAt = day3, updatedAt = day3, lastTimelineItemUuid = sanDailyUuid)),
          )
          else -> null
        }
      }

      val response = service.getVersionsByEntityId(sanUuid)

      val queries = dispatchedQueries()
      assertThat(queries.map { it::class to it.assessmentIdentifier.uuid }).containsExactly(
        DailyVersionsQuery::class to planUuid,
        TimelineQuery::class to planUuid,
        DailyVersionsQuery::class to sanUuid,
      )
      assertThat((queries[1] as TimelineQuery).includeCustomTypes).containsExactly("PLAN_AGREEMENT_STATUS_CHANGED")

      assertThat(response.unaggregatedVersions).containsExactlyInAnyOrder(
        VersionDetails(
          uuid = lockedPlanVersion.uuid,
          version = epoch(day1),
          createdAt = day1,
          updatedAt = day1,
          status = "LOCKED",
          planAgreementStatus = "",
          entityType = EntityType.AAP_PLAN,
        ),
        VersionDetails(
          uuid = agreedTimelineUuid,
          version = epoch(day2),
          createdAt = day2,
          updatedAt = day2,
          status = "UNSIGNED",
          planAgreementStatus = "AGREED",
          entityType = EntityType.AAP_PLAN,
        ),
        VersionDetails(
          uuid = sanDailyUuid,
          version = epoch(day3),
          createdAt = day3,
          updatedAt = day3,
          status = "UNSIGNED",
          planAgreementStatus = null,
          entityType = EntityType.AAP_SAN,
        ),
      )
    }

    @Test
    fun `uses the update time as the version of legacy local versions`() {
      whenever(oasysAssociationsService.findOasysPkByEntityId(sanUuid)).thenReturn(oasysAssessmentPk)
      givenAssociations(association(EntityType.AAP_SAN))
      whenever(oasysVersionService.fetchAllForEntityUuid(sanUuid)).thenReturn(
        listOf(OasysVersionEntity(createdBy = OasysEvent.SELF_SIGNED, version = 3, entityUuid = sanUuid, createdAt = day1, updatedAt = day2)),
      )
      queryResults.add { DailyVersionsQueryResult(emptyList()) }

      val version = service.getVersionsByEntityId(sanUuid).unaggregatedVersions.single()

      assertThat(version.version).isEqualTo(epoch(day2))
      assertThat(version.status).isEqualTo("SELF_SIGNED")
    }
  }

  @Nested
  inner class GetAssociations {
    @Test
    fun `throws not found when there are no associations`() {
      givenAssociations()

      assertThrows<NoAssociationsException> { service.getAssociations(oasysAssessmentPk) }
    }

    @Test
    fun `returns the entity ids without versions when no versions are requested`() {
      givenAssociations(association(EntityType.AAP_PLAN), association(EntityType.AAP_SAN))

      val response = service.getAssociations(oasysAssessmentPk)

      assertThat(response.sentencePlanId).isEqualTo(planUuid)
      assertThat(response.sanAssessmentId).isEqualTo(sanUuid)
      assertThat(response.sentencePlanVersion).isNull()
      assertThat(response.sanAssessmentVersion).isNull()
      verify(oasysVersionService, never()).fetchVersion(any(), any())
    }

    @Test
    fun `returns the epoch timestamps of the requested versions`() {
      givenAssociations(association(EntityType.AAP_PLAN), association(EntityType.AAP_SAN))
      val planCreatedAt = LocalDateTime.parse("2026-01-01T10:00:00")
      val sanCreatedAt = LocalDateTime.parse("2026-01-02T10:00:00")
      whenever(oasysVersionService.fetchVersion(planUuid, 5)).thenReturn(
        OasysVersionEntity(createdBy = OasysEvent.CREATED, version = 5, entityUuid = planUuid, createdAt = planCreatedAt),
      )
      whenever(oasysVersionService.fetchVersion(sanUuid, 7)).thenReturn(
        OasysVersionEntity(createdBy = OasysEvent.CREATED, version = 7, entityUuid = sanUuid, createdAt = sanCreatedAt),
      )

      val response = service.getAssociations(oasysAssessmentPk, planVersion = 5, assessmentVersion = 7)

      assertThat(response.sentencePlanVersion).isEqualTo(planCreatedAt.toInstant(ZoneOffset.UTC).toEpochMilli())
      assertThat(response.sanAssessmentVersion).isEqualTo(sanCreatedAt.toInstant(ZoneOffset.UTC).toEpochMilli())
    }

    @Test
    fun `throws when the requested plan version does not exist`() {
      givenAssociations(association(EntityType.AAP_PLAN))
      whenever(oasysVersionService.fetchVersion(planUuid, 5)).thenReturn(null)

      val exception = assertThrows<CoordinatorException> { service.getAssociations(oasysAssessmentPk, planVersion = 5) }
      assertThat(exception.message).isEqualTo("OASys plan version not found for the provided entityUuid")
    }

    @Test
    fun `throws when the requested SAN version does not exist`() {
      givenAssociations(association(EntityType.AAP_SAN))
      whenever(oasysVersionService.fetchVersion(sanUuid, 7)).thenReturn(null)

      val exception = assertThrows<CoordinatorException> { service.getAssociations(oasysAssessmentPk, assessmentVersion = 7) }
      assertThat(exception.message).isEqualTo("OASys SAN version not found for the provided entityUuid")
    }
  }

  @Nested
  inner class Lock {
    @Test
    fun `throws not found when there are no associations`() {
      givenAssociations()

      assertThrows<NoAssociationsException> { service.lock(OasysGenericRequest(userDetails), oasysAssessmentPk) }
      assertThat(publishedEvents).isEmpty()
    }

    @Test
    fun `creates a LOCKED version of each entity and publishes a plan event`() {
      givenAssociations(association(EntityType.AAP_PLAN), association(EntityType.AAP_SAN))

      val response = service.lock(OasysGenericRequest(userDetails), oasysAssessmentPk)

      assertThat(createdVersions.map { it.createdBy to it.entityUuid })
        .containsExactly(OasysEvent.LOCKED to planUuid, OasysEvent.LOCKED to sanUuid)
      assertThat(response.sentencePlanVersion).isEqualTo(createdVersions[0].version)
      assertThat(response.sanAssessmentVersion).isEqualTo(createdVersions[1].version)
      assertThat(versionPayloads().single().oasysEvent).isEqualTo(OasysEvent.LOCKED)
    }

    @Test
    fun `does not publish an event when there is no plan`() {
      givenAssociations(association(EntityType.AAP_SAN))

      service.lock(OasysGenericRequest(userDetails), oasysAssessmentPk)

      assertThat(publishedEvents).isEmpty()
    }
  }

  @Nested
  inner class Sign {
    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.arnscoordinatorapi.service.OasysCoordinatorServiceTest#signOutcomes")
    fun `creates a signed version of each entity and publishes a plan event`(signType: SignType, expectedEvent: OasysEvent) {
      givenAssociations(association(EntityType.AAP_PLAN), association(EntityType.AAP_SAN))

      val response = service.sign(OasysSignRequest(signType = signType, userDetails = userDetails), oasysAssessmentPk)

      assertThat(createdVersions.map { it.createdBy to it.entityUuid })
        .containsExactly(expectedEvent to planUuid, expectedEvent to sanUuid)
      assertThat(response.sentencePlanId).isEqualTo(planUuid)
      assertThat(response.sanAssessmentId).isEqualTo(sanUuid)
      assertThat(versionPayloads().single().oasysEvent).isEqualTo(expectedEvent)
    }

    @Test
    fun `throws not found when there are no associations`() {
      givenAssociations()

      assertThrows<NoAssociationsException> {
        service.sign(OasysSignRequest(signType = SignType.SELF, userDetails = userDetails), oasysAssessmentPk)
      }
      assertThat(publishedEvents).isEmpty()
    }
  }

  @Nested
  inner class Rollback {
    @Test
    fun `marks the requested version of each entity as ROLLED_BACK and publishes a plan event`() {
      givenAssociations(association(EntityType.AAP_PLAN), association(EntityType.AAP_SAN))

      val response = service.rollback(
        OasysRollbackRequest(sanVersionNumber = 11, sentencePlanVersionNumber = 22, userDetails = userDetails),
        oasysAssessmentPk,
      )

      verify(oasysVersionService).updateVersion(OasysEvent.ROLLED_BACK, planUuid, 22)
      verify(oasysVersionService).updateVersion(OasysEvent.ROLLED_BACK, sanUuid, 11)
      assertThat(response.sentencePlanVersion).isEqualTo(22)
      assertThat(response.sanAssessmentVersion).isEqualTo(11)
      assertThat(versionPayloads().single().oasysEvent).isEqualTo(OasysEvent.ROLLED_BACK)
    }

    @Test
    fun `throws when the request has no version for an associated entity`() {
      givenAssociations(association(EntityType.AAP_PLAN), association(EntityType.AAP_SAN))

      val exception = assertThrows<CoordinatorException> {
        service.rollback(OasysRollbackRequest(sanVersionNumber = null, sentencePlanVersionNumber = 22, userDetails = userDetails), oasysAssessmentPk)
      }

      assertThat(exception.message).isEqualTo("AAP_SAN version does not exist on the request")
      assertThat(publishedEvents).isEmpty()
    }

    @Test
    fun `throws not found when there are no associations`() {
      givenAssociations()

      assertThrows<NoAssociationsException> {
        service.rollback(OasysRollbackRequest(sanVersionNumber = 1, sentencePlanVersionNumber = 1, userDetails = userDetails), oasysAssessmentPk)
      }
    }
  }

  @Nested
  inner class CounterSign {
    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.arnscoordinatorapi.service.OasysCoordinatorServiceTest#counterSignOutcomes")
    fun `marks the requested version of each entity with the outcome and publishes a plan event`(outcome: CounterSignOutcome, expectedEvent: OasysEvent) {
      givenAssociations(association(EntityType.AAP_PLAN), association(EntityType.AAP_SAN))

      val response = service.counterSign(
        oasysAssessmentPk,
        OasysCounterSignRequest(sanVersionNumber = 11, sentencePlanVersionNumber = 22, outcome = outcome, userDetails = userDetails),
      )

      verify(oasysVersionService).updateVersion(expectedEvent, planUuid, 22)
      verify(oasysVersionService).updateVersion(expectedEvent, sanUuid, 11)
      assertThat(response.sentencePlanVersion).isEqualTo(22)
      assertThat(response.sanAssessmentVersion).isEqualTo(11)
      assertThat(versionPayloads().single().oasysEvent).isEqualTo(expectedEvent)
    }

    @Test
    fun `throws when the request has no version for an associated entity`() {
      givenAssociations(association(EntityType.AAP_PLAN))

      val exception = assertThrows<CoordinatorException> {
        service.counterSign(
          oasysAssessmentPk,
          OasysCounterSignRequest(sanVersionNumber = 11, sentencePlanVersionNumber = null, outcome = CounterSignOutcome.COUNTERSIGNED, userDetails = userDetails),
        )
      }

      assertThat(exception.message).isEqualTo("Unable to countersign, no version number provided for AAP_PLAN entity $planUuid")
    }

    @Test
    fun `throws not found when there are no associations`() {
      givenAssociations()

      assertThrows<NoAssociationsException> {
        service.counterSign(
          oasysAssessmentPk,
          OasysCounterSignRequest(sanVersionNumber = 1, sentencePlanVersionNumber = 1, outcome = CounterSignOutcome.COUNTERSIGNED, userDetails = userDetails),
        )
      }
    }
  }

  @Nested
  inner class SoftDelete {
    private val request = OasysGenericRequest(userDetails)

    @Test
    fun `throws not found when there are no associations`() {
      givenAssociations()

      assertThrows<NoAssociationsException> { service.softDelete(request, oasysAssessmentPk) }
      verify(aapApi, never()).dispatchCommands(any())
    }

    @Test
    fun `soft-deletes the AAP assessment and local versions of the latest association`() {
      val baseVersion = LocalDateTime.parse("2026-01-01T09:00:00").toInstant(ZoneOffset.UTC).toEpochMilli()
      val plan = association(EntityType.AAP_PLAN, baseVersion = baseVersion)
      val san = association(EntityType.AAP_SAN, baseVersion = baseVersion)
      givenAssociations(plan, san)
      whenever(oasysAssociationsService.findAllIncludingDeleted(any())).thenReturn(emptyList())

      val response = service.softDelete(request, oasysAssessmentPk)

      assertThat(dispatchedCommands()).containsExactly(
        SoftDeleteAssessmentCommand(assessmentUuid = planUuid, user = aapUser, pointInTime = LocalDateTime.parse("2026-01-01T09:00:00")),
        SoftDeleteAssessmentCommand(assessmentUuid = sanUuid, user = aapUser, pointInTime = LocalDateTime.parse("2026-01-01T09:00:00")),
      )
      verify(oasysVersionService).softDeleteVersions(planUuid, baseVersion, null)
      verify(oasysVersionService).softDeleteVersions(sanUuid, baseVersion, null)
      assertThat(plan.deleted).isTrue()
      assertThat(san.deleted).isTrue()
      assertThat(response.sentencePlanId).isEqualTo(planUuid)
      assertThat(response.sanAssessmentId).isEqualTo(sanUuid)

      assertThat(publishedEvents.single().message).isEqualTo(DeleteFlagUpdatePayload(deleted = true, versionFrom = baseVersion, versionTo = null))
    }

    @Test
    fun `only soft-deletes local versions up to the next association for an older association`() {
      val plan = association(EntityType.AAP_PLAN, baseVersion = 100, createdAt = now.minusDays(2))
      givenAssociations(plan)
      whenever(oasysAssociationsService.findAllIncludingDeleted(planUuid)).thenReturn(
        listOf(plan, association(EntityType.AAP_PLAN, oasysAssessmentPk = "2002", baseVersion = 200, createdAt = now.minusDays(1))),
      )

      service.softDelete(request, oasysAssessmentPk)

      verify(oasysVersionService).softDeleteVersions(planUuid, 100, 200)
      verify(aapApi, never()).dispatchCommands(any())
      assertThat(plan.deleted).isTrue()
      assertThat(publishedEvents.single().message).isEqualTo(DeleteFlagUpdatePayload(deleted = true, versionFrom = 100, versionTo = 200))
    }

    @Test
    fun `does not publish an event when AAP fails`() {
      givenAssociations(association(EntityType.AAP_PLAN))
      whenever(oasysAssociationsService.findAllIncludingDeleted(any())).thenReturn(emptyList())
      whenever(aapApi.dispatchCommands(any())).thenThrow(CoordinatorException("AAP is down"))

      assertThrows<CoordinatorException> { service.softDelete(request, oasysAssessmentPk) }

      assertThat(publishedEvents).isEmpty()
    }
  }

  @Nested
  inner class Undelete {
    private val request = OasysGenericRequest(userDetails)

    @Test
    fun `throws not found when there are no deleted associations`() {
      whenever(oasysAssociationsService.findDeletedAssociations(oasysAssessmentPk)).thenReturn(emptyList())

      assertThrows<NoDeletedAssociationsException> { service.undelete(request, oasysAssessmentPk) }
    }

    @Test
    fun `undeletes the AAP assessment and local versions of the latest association`() {
      val baseVersion = LocalDateTime.parse("2026-01-01T09:00:00").toInstant(ZoneOffset.UTC).toEpochMilli()
      val plan = association(EntityType.AAP_PLAN, baseVersion = baseVersion, deleted = true)
      whenever(oasysAssociationsService.findDeletedAssociations(oasysAssessmentPk)).thenReturn(listOf(plan))
      whenever(oasysAssociationsService.findAllIncludingDeleted(planUuid)).thenReturn(listOf(plan))
      whenever(oasysVersionService.findDeletedVersions(planUuid, baseVersion, null)).thenReturn(
        listOf(OasysVersionEntity(createdBy = OasysEvent.CREATED, version = baseVersion, entityUuid = planUuid, deleted = true)),
      )

      val response = service.undelete(request, oasysAssessmentPk)

      assertThat(dispatchedCommands()).containsExactly(
        UndeleteAssessmentCommand(assessmentUuid = planUuid, user = aapUser, pointInTime = LocalDateTime.parse("2026-01-01T09:00:00")),
      )
      verify(oasysVersionService).undeleteVersions(planUuid, baseVersion, null)
      assertThat(plan.deleted).isFalse()
      assertThat(response.sentencePlanId).isEqualTo(planUuid)
      assertThat(publishedEvents.single().message).isEqualTo(DeleteFlagUpdatePayload(deleted = false, versionFrom = baseVersion, versionTo = null))
    }

    @Test
    fun `throws a conflict without calling AAP when the latest association has no deleted versions`() {
      val plan = association(EntityType.AAP_PLAN, deleted = true)
      whenever(oasysAssociationsService.findDeletedAssociations(oasysAssessmentPk)).thenReturn(listOf(plan))
      whenever(oasysAssociationsService.findAllIncludingDeleted(planUuid)).thenReturn(listOf(plan))
      whenever(oasysVersionService.findDeletedVersions(planUuid, 0, null)).thenReturn(emptyList())

      val exception = assertThrows<ConflictException> { service.undelete(request, oasysAssessmentPk) }

      assertThat(exception.message).isEqualTo("No deleted versions found to undelete for entity $planUuid")
      verify(aapApi, never()).dispatchCommands(any())
      verify(oasysVersionService, never()).undeleteVersions(any(), any(), anyOrNull())
      assertThat(plan.deleted).isTrue()
    }

    @Test
    fun `only undeletes local versions up to the next association for an older association`() {
      val plan = association(EntityType.AAP_PLAN, baseVersion = 100, createdAt = now.minusDays(2), deleted = true)
      whenever(oasysAssociationsService.findDeletedAssociations(oasysAssessmentPk)).thenReturn(listOf(plan))
      whenever(oasysAssociationsService.findAllIncludingDeleted(planUuid)).thenReturn(
        listOf(plan, association(EntityType.AAP_PLAN, oasysAssessmentPk = "2002", baseVersion = 200, createdAt = now.minusDays(1))),
      )

      service.undelete(request, oasysAssessmentPk)

      verify(oasysVersionService).undeleteVersions(planUuid, 100, 200)
      verify(oasysVersionService, never()).findDeletedVersions(any(), any(), anyOrNull())
      verify(aapApi, never()).dispatchCommands(any())
      assertThat(plan.deleted).isFalse()
    }
  }

  @Nested
  inner class Merge {
    private fun request(vararg pairs: Pair<String, String>) = OasysMergeRequest(
      merge = pairs.map { (old, new) -> OasysTransferAssociation(oldOasysAssessmentPK = old, newOasysAssessmentPK = new) },
      userDetails = userDetails,
    )

    @Test
    fun `moves the associations to the new PK and marks the entities as merged`() {
      val plan = association(EntityType.AAP_PLAN, oasysAssessmentPk = "1")
      val san = association(EntityType.AAP_SAN, oasysAssessmentPk = "1")
      whenever(oasysAssociationsService.findAssociationsByPk("2")).thenReturn(emptyList())
      whenever(oasysAssociationsService.findAssociationsByPk("1", includeDeleted = true)).thenReturn(listOf(plan, san))

      val response = service.merge(request("1" to "2"))

      assertThat(response.message).isEqualTo("Successfully processed all 1 merge elements")
      assertThat(plan.oasysAssessmentPk).isEqualTo("2")
      assertThat(san.oasysAssessmentPk).isEqualTo("2")
      verify(oasysAssociationsService, times(2)).save(any())
      assertThat(dispatchedCommands()).containsExactly(
        UpdateAssessmentPropertiesCommand(
          assessmentUuid = planUuid,
          user = aapUser,
          added = mapOf("MERGED" to PropertyValue(type = "Single", value = "Y")),
          timeline = Timeline(type = "MERGED"),
        ),
        UpdateAssessmentPropertiesCommand(
          assessmentUuid = sanUuid,
          user = aapUser,
          added = mapOf("MERGED" to PropertyValue(type = "Single", value = "Y")),
          timeline = Timeline(type = "MERGED"),
        ),
      )
    }

    @Test
    fun `throws not found listing every old PK without associations`() {
      whenever(oasysAssociationsService.findAssociationsByPk(any(), eq(false))).thenReturn(emptyList())
      whenever(oasysAssociationsService.findAssociationsByPk(any(), eq(true))).thenReturn(emptyList())

      val exception = assertThrows<NoAssociationsException> { service.merge(request("1" to "2", "3" to "4")) }

      assertThat(exception.message).isEqualTo("The following association(s) could not be located: 1, 3 and the operation has not been actioned.")
      verify(aapApi, never()).dispatchCommands(any())
    }

    @Test
    fun `throws a conflict listing every new PK that already has associations`() {
      whenever(oasysAssociationsService.findAssociationsByPk(any(), eq(false))).thenReturn(listOf(association(EntityType.AAP_PLAN)))
      whenever(oasysAssociationsService.findAssociationsByPk(any(), eq(true))).thenReturn(listOf(association(EntityType.AAP_PLAN)))

      val exception = assertThrows<ConflictException> { service.merge(request("1" to "2", "3" to "4")) }

      assertThat(exception.message).isEqualTo("Existing association(s) for 2, 4")
      verify(oasysAssociationsService, never()).save(any())
      verify(aapApi, never()).dispatchCommands(any())
    }
  }

  companion object {
    @JvmStatic
    fun signOutcomes(): Stream<Arguments> = Stream.of(
      Arguments.of(SignType.SELF, OasysEvent.SELF_SIGNED),
      Arguments.of(SignType.COUNTERSIGN, OasysEvent.AWAITING_COUNTERSIGN),
    )

    @JvmStatic
    fun counterSignOutcomes(): Stream<Arguments> = Stream.of(
      Arguments.of(CounterSignOutcome.COUNTERSIGNED, OasysEvent.COUNTERSIGNED),
      Arguments.of(CounterSignOutcome.AWAITING_DOUBLE_COUNTERSIGN, OasysEvent.AWAITING_DOUBLE_COUNTERSIGN),
      Arguments.of(CounterSignOutcome.DOUBLE_COUNTERSIGNED, OasysEvent.DOUBLE_COUNTERSIGNED),
      Arguments.of(CounterSignOutcome.REJECTED, OasysEvent.REJECTED),
    )
  }
}

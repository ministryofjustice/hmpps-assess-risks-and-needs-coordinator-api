package uk.gov.justice.digital.hmpps.arnscoordinatorapi.service

import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysMergeRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.SignType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysAssociationsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysMessageResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.VersionsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.VersionsResponseFactory
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.OasysEventFactory
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.OasysEventPublisher
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.CoordinatorException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.MissingEntityTypeException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.NoAssociationsException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.NoAssociationsForEntityException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.NoAssociationsForEntityUuidException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.NoDeletedAssociationsException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.AAPApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.Command
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
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.DailyVersionsQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.QueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.TimelineQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.VersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysAssociationEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysEvent
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class OasysCoordinatorService(
  private val sentencePlanService: SentencePlanService,
  private val strengthsAndNeedsService: StrengthsAndNeedsService,
  private val oasysAssociationsService: OasysAssociationsService,
  private val oasysVersionService: OasysVersionService,
  private val oasysEventPublisher: OasysEventPublisher,
  private val oasysEventFactory: OasysEventFactory,
  private val aapApi: AAPApi,
  private val clock: Clock,
) {
  val coordinatorUser = AAPUser(id = "COORDINATOR_API", name = "Coordinator API User")

  @Transactional
  fun create(requestData: OasysCreateRequest): OasysVersionedEntityResponse {
    oasysAssociationsService
      .getExistingAssociationTypes(requestData.oasysAssessmentPk)
      .takeIf { it.isNotEmpty() }
      ?.let {
        throw ConflictException("Cannot create due to conflicting associations: ${it.joinToString(", ")}")
      }

    val commands = mutableListOf<Command>()

    val resetPlan = requestData.newPeriodOfSupervision == "Y"

    val createSPCommand = if (requestData.previousOasysSpPk == null && !resetPlan) {
      sentencePlanService.create(requestData).also(commands::add)
    } else {
      null
    }

    val createSANCommand = if (requestData.previousOasysSanPk == null && requestData.assessmentType == AssessmentType.SAN_SP) {
      strengthsAndNeedsService.create(requestData).also(commands::add)
    } else {
      null
    }

    val response = OasysVersionedEntityResponse()

    val onAfter = mutableListOf<() -> Unit>()

    // Process associations/versions for existing AAP assessment(s)
    listOfNotNull(
      (createSPCommand ?: EntityType.AAP_PLAN).takeIf { it is EntityType } as EntityType?,
      (createSANCommand ?: EntityType.AAP_SAN).takeIf { it is EntityType } as EntityType?,
    ).forEach { entityType ->
      val existingAssociation = oasysAssociationsService
        .findAssociationsByPkAndType(requestData.previousPkFor(entityType), entityType)
        .firstOrNull()
        ?: throw NoAssociationsForEntityException(
          entityType = entityType,
          oasysPk = requestData.previousPkFor(entityType),
        )

      val oasysEvent = when {
        entityType == EntityType.AAP_PLAN && resetPlan ->
          OasysEvent.CREATED
            .also {
              sentencePlanService
                .reset(requestData, existingAssociation.entityUuid)
                .run(commands::add)
            }
        else -> OasysEvent.CLONED
      }

      UpdateFlagsCommand(
        assessmentUuid = existingAssociation.entityUuid,
        flags = requestData.assessmentType.toFlags(),
        user = requestData.userDetails.toAAPUser(),
        timeline = Timeline(type = "FLAGS_UPDATED"),
      ).run(commands::add)

      onAfter.add {
        OasysAssociationEntity(
          oasysAssessmentPk = requestData.oasysAssessmentPk,
          entityType = entityType,
          entityUuid = existingAssociation.entityUuid,
          baseVersion = oasysVersionService
            .createVersionFor(oasysEvent, existingAssociation.entityUuid)
            .also { response.populate(it, entityType) }
            .version,
          regionPrisonCode = requestData.regionPrisonCode,
        ).run(oasysAssociationsService::save)
      }
    }

    val commandsResponse = aapApi.dispatchCommands(commands)

    // Process associations/versions for new AAP assessment(s)
    listOfNotNull(
      createSPCommand?.let { Pair(createSPCommand, commandsResponse.extractNthInstance<CreateAssessmentCommandResult>(0)) },
      createSANCommand?.let { Pair(createSANCommand, commandsResponse.extractNthInstance<CreateAssessmentCommandResult>(1)) },
    ).forEach { (command, result) ->
      OasysAssociationEntity(
        oasysAssessmentPk = requestData.oasysAssessmentPk,
        entityType = EntityType.fromAAPAssessmentType(command.assessmentType),
        entityUuid = result.assessmentUuid,
        regionPrisonCode = requestData.regionPrisonCode,
      ).also(oasysAssociationsService::save)

      oasysVersionService.createVersionFor(OasysEvent.CREATED, result.assessmentUuid)
        .also { response.populate(it, EntityType.fromAAPAssessmentType(command.assessmentType)) }
    }

    onAfter.forEach { closure -> closure.invoke() }

    oasysEventPublisher.publish(oasysEventFactory.createVersionEvent(requestData, response))

    return response
  }

  @Transactional
  fun get(oasysAssessmentPk: String): OasysGetResponse {
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

    if (associations.isEmpty()) {
      throw NoAssociationsException(oasysAssessmentPk)
    }

    fun assessmentVersionQuery(entityUuid: UUID) = AssessmentVersionQuery(
      user = coordinatorUser,
      assessmentIdentifier = AssessmentIdentifier(entityUuid),
      timestamp = clock.now(),
    )

    val spQuery = associations.firstOrNull { it.entityType == EntityType.AAP_PLAN }
      ?.let { assessmentVersionQuery(it.entityUuid) }

    val sanQuery = associations.firstOrNull { it.entityType == EntityType.AAP_SAN }
      ?.let { assessmentVersionQuery(it.entityUuid) }

    val queriesResponse = aapApi.dispatchQueries(listOfNotNull(spQuery, sanQuery))

    val response = OasysGetResponse()

    listOfNotNull(
      spQuery?.let { Pair(EntityType.AAP_PLAN, queriesResponse.extractNthInstance<AssessmentVersionQueryResult>(0)) },
      sanQuery?.let { Pair(EntityType.AAP_SAN, queriesResponse.extractNthInstance<AssessmentVersionQueryResult>(1)) },
    ).forEach { (entityType, data) ->
      when (entityType) {
        EntityType.AAP_PLAN -> sentencePlanService.fetch(data, response)
        EntityType.AAP_SAN -> strengthsAndNeedsService.fetch(data, response)
      }
    }

    return response
  }

  @Transactional
  fun lock(
    request: OasysGenericRequest,
    oasysAssessmentPk: String,
  ): OasysVersionedEntityResponse {
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

    if (associations.isEmpty()) {
      throw NoAssociationsException(oasysAssessmentPk)
    }

    val response = OasysVersionedEntityResponse()

    associations.forEach { association ->
      oasysVersionService.createVersionFor(OasysEvent.LOCKED, association.entityUuid)
        .also { response.populate(it, association.entityType ?: throw MissingEntityTypeException(association)) }
    }

    associations.firstOrNull { it.entityType == EntityType.AAP_PLAN }?.let { planAssociation ->
      oasysEventPublisher.publish(oasysEventFactory.lockVersionEvent(planAssociation, response))
    }

    log.info("Assessments locked by user ${request.userDetails.id}: ${associations.map { it.entityUuid }.joinToString()}")

    return response
  }

  @Transactional
  fun sign(
    request: OasysSignRequest,
    oasysAssessmentPk: String,
  ): OasysVersionedEntityResponse {
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

    if (associations.isEmpty()) {
      throw NoAssociationsException(oasysAssessmentPk)
    }

    val response = OasysVersionedEntityResponse()

    associations.forEach { association ->
      when (request.signType) {
        SignType.SELF -> oasysVersionService.createVersionFor(OasysEvent.SELF_SIGNED, association.entityUuid)
        SignType.COUNTERSIGN -> oasysVersionService.createVersionFor(OasysEvent.AWAITING_COUNTERSIGN, association.entityUuid)
      }.also { oasysVersion ->
        response.populate(oasysVersion, association.entityType ?: throw MissingEntityTypeException(association))
      }
    }

    associations.firstOrNull { it.entityType == EntityType.AAP_PLAN }?.let { planAssociation ->
      oasysEventPublisher.publish(
        oasysEventFactory.signVersionEvent(planAssociation, request, response),
      )
    }

    return response
  }

  @Transactional
  fun rollback(
    request: OasysRollbackRequest,
    oasysAssessmentPk: String,
  ): OasysVersionedEntityResponse {
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

    if (associations.isEmpty()) {
      throw NoAssociationsException(oasysAssessmentPk)
    }

    val response = OasysVersionedEntityResponse()

    associations.forEach { association ->
      val entityType = association.entityType ?: throw MissingEntityTypeException(association)
      val version = when (entityType) {
        EntityType.AAP_PLAN -> request.sentencePlanVersionNumber
        EntityType.AAP_SAN -> request.sanVersionNumber
      } ?: throw CoordinatorException("$entityType version does not exist on the request")

      oasysVersionService.updateVersion(OasysEvent.ROLLED_BACK, association.entityUuid, version)
        .also { response.populate(it, association.entityType ?: throw MissingEntityTypeException(association)) }
    }

    associations.firstOrNull { it.entityType == EntityType.AAP_PLAN }?.let { planAssociation ->
      oasysEventPublisher.publish(oasysEventFactory.rollbackVersionEvent(planAssociation, response))
    }

    return response
  }

  @Transactional
  fun getByEntityId(entityUuid: UUID, entityType: EntityType): OasysGetResponse {
    val oasysAssessmentPk = oasysAssociationsService.findOasysPkByEntityId(entityUuid)
      ?: throw NoAssociationsForEntityUuidException(entityUuid)

    oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)
      .firstOrNull { it.entityType == entityType }
      ?: throw NoAssociationsForEntityException(entityType, oasysAssessmentPk)

    return get(oasysAssessmentPk)
  }

  @Transactional
  fun getVersionsByEntityId(entityUuid: UUID): VersionsResponse {
    val oasysAssessmentPk = oasysAssociationsService.findOasysPkByEntityId(entityUuid)
      ?: throw NoAssociationsForEntityUuidException(entityUuid)

    val versionsResponseFactory = VersionsResponseFactory()

    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

    val queries = mutableListOf<Pair<Query, (QueryResult) -> Unit>>()

    val oasysVersionsByType = mutableMapOf<EntityType, List<VersionDetails>>()
    val aapVersionsByType = mutableMapOf<EntityType, List<VersionDetails>>()

    associations.forEach { association ->
      val entityType = association.entityType ?: throw MissingEntityTypeException(association)

      oasysVersionsByType[entityType] = oasysVersionService.fetchAllForEntityUuid(entityUuid).map {
        VersionDetails(
          uuid = it.uuid,
          version = if (it.version > 1000L) it.version else it.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli(),
          createdAt = it.createdAt,
          updatedAt = it.updatedAt,
          status = it.createdBy.name,
          planAgreementStatus = if (entityType == EntityType.AAP_PLAN) "" else null,
          entityType = entityType,
        )
      }

      queries.add(
        DailyVersionsQuery(
          user = coordinatorUser,
          assessmentIdentifier = AssessmentIdentifier(entityUuid),
        ) to { result: QueryResult ->
          aapVersionsByType[entityType] = (result as DailyVersionsQueryResult).versions.map {
            VersionDetails(
              uuid = it.lastTimelineItemUuid,
              version = it.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli(),
              createdAt = it.createdAt,
              updatedAt = it.updatedAt,
              status = "UNSIGNED",
              planAgreementStatus = if (entityType == EntityType.AAP_PLAN) "" else null,
              entityType = entityType,
            )
          }
        },
      )

      if (entityType == EntityType.AAP_PLAN) {
        queries.add(
          TimelineQuery(
            user = coordinatorUser,
            assessmentIdentifier = AssessmentIdentifier(entityUuid),
            pageSize = 99999,
            includeCustomTypes = setOf("PLAN_AGREEMENT_STATUS_CHANGED"),
          ) to { result: QueryResult ->
            aapVersionsByType[entityType] = (result as TimelineQueryResult).timeline
              .filter { it.customData?.get("status") == "AGREED" }
              .map { timelineItem ->
                VersionDetails(
                  uuid = timelineItem.uuid,
                  version = timelineItem.timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                  createdAt = timelineItem.timestamp,
                  updatedAt = timelineItem.timestamp,
                  status = "UNSIGNED",
                  planAgreementStatus = "AGREED",
                  entityType = entityType,
                )
              }.plus(aapVersionsByType[entityType].orEmpty())
          },
        )
      }
    }

    aapApi.dispatchQueries(queries)

    oasysVersionsByType.forEach { (entityType, oasysVersions) ->
      (oasysVersions + aapVersionsByType[entityType].orEmpty())
        .groupBy { it.updatedAt }
        .values
        .map { versions ->
          versions.reduce { acc, next ->
            acc.copy(
              status = acc.status.ifBlank { next.status },
              planAgreementStatus = when (entityType) {
                EntityType.AAP_PLAN -> acc.planAgreementStatus?.takeIf { it.isNotBlank() } ?: next.planAgreementStatus
                else -> null
              },
            )
          }
        }
        .run(versionsResponseFactory::addVersions)
    }

    return versionsResponseFactory.getVersionsResponse()
  }

  @Transactional
  fun getAssociations(oasysAssessmentPk: String, planVersion: Long? = null, assessmentVersion: Long? = null): OasysAssociationsResponse {
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

    if (associations.isEmpty()) {
      throw NoAssociationsException(oasysAssessmentPk)
    }

    val response = OasysAssociationsResponse()

    associations.forEach { association ->
      when (association.entityType) {
        EntityType.AAP_SAN -> response.apply {
          sanAssessmentId = association.entityUuid
          assessmentVersion?.let { version ->
            sanAssessmentVersion = oasysVersionService.fetchVersion(association.entityUuid, version)?.getEpochVersion()
              ?: throw CoordinatorException("OASys SAN version not found for the provided entityUuid")
          }
        }

        EntityType.AAP_PLAN -> response.apply {
          sentencePlanId = association.entityUuid
          planVersion?.let { version ->
            sentencePlanVersion = oasysVersionService.fetchVersion(association.entityUuid, version)?.getEpochVersion()
              ?: throw CoordinatorException("OASys plan version not found for the provided entityUuid")
          }
        }

        null -> throw MissingEntityTypeException(association)
      }
    }

    return response
  }

  @Transactional
  fun counterSign(
    oasysAssessmentPk: String,
    request: OasysCounterSignRequest,
  ): OasysVersionedEntityResponse {
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

    if (associations.isEmpty()) {
      throw NoAssociationsException(oasysAssessmentPk)
    }

    val response = OasysVersionedEntityResponse()

    associations.forEach { association ->
      val entityType = association.entityType ?: throw MissingEntityTypeException(association)
      val version = when (entityType) {
        EntityType.AAP_PLAN -> request.sentencePlanVersionNumber
        EntityType.AAP_SAN -> request.sanVersionNumber
      } ?: throw CoordinatorException("Unable to countersign, no version number provided for $entityType entity ${association.entityUuid}")

      when (request.outcome) {
        CounterSignOutcome.COUNTERSIGNED -> oasysVersionService.updateVersion(
          OasysEvent.COUNTERSIGNED,
          association.entityUuid,
          version,
        )

        CounterSignOutcome.AWAITING_DOUBLE_COUNTERSIGN -> oasysVersionService.updateVersion(
          OasysEvent.AWAITING_DOUBLE_COUNTERSIGN,
          association.entityUuid,
          version,
        )

        CounterSignOutcome.DOUBLE_COUNTERSIGNED -> oasysVersionService.updateVersion(
          OasysEvent.DOUBLE_COUNTERSIGNED,
          association.entityUuid,
          version,
        )

        CounterSignOutcome.REJECTED -> oasysVersionService.updateVersion(OasysEvent.REJECTED, association.entityUuid, version)
      }.also { oasysVersion ->
        response.populate(oasysVersion, entityType)
      }
    }

    associations.firstOrNull { it.entityType == EntityType.AAP_PLAN }?.let { planAssociation ->
      oasysEventPublisher.publish(oasysEventFactory.counterSignVersionEvent(planAssociation, request, response))
    }

    return response
  }

  @Transactional
  fun softDelete(
    request: OasysGenericRequest,
    oasysAssessmentPk: String,
  ): OasysVersionedEntityResponse {
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

    if (associations.isEmpty()) {
      throw NoAssociationsException(oasysAssessmentPk)
    }

    val response = OasysVersionedEntityResponse()
    val commands = mutableListOf<Command>()
    val onSuccess = mutableListOf<() -> Unit>()

    associations.forEach { association ->
      val entityType = association.entityType ?: throw MissingEntityTypeException(association)

      val versionFrom = association.baseVersion
      val versionTo = oasysAssociationsService
        .findAllIncludingDeleted(association.entityUuid)
        .filter { it.createdAt > association.createdAt }
        .minByOrNull { it.createdAt }?.baseVersion

      if (versionTo == null) {
        SoftDeleteAssessmentCommand(
          assessmentUuid = association.entityUuid,
          user = request.userDetails.toAAPUser(),
          pointInTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(versionFrom), ZoneOffset.UTC),
        ).run(commands::add)
      }

      oasysVersionService.softDeleteVersions(association.entityUuid, versionFrom, versionTo)
        .also { response.populate(it, entityType) }

      association.apply { deleted = true }.run(oasysAssociationsService::save)

      if (entityType == EntityType.AAP_PLAN) {
        onSuccess.add {
          oasysEventPublisher.publish(oasysEventFactory.softDeleteEvent(association, versionTo))
        }
      }
    }

    commands.ifEmpty { null }?.run(aapApi::dispatchCommands)
    onSuccess.forEach { it() }

    return response
  }

  @Transactional
  fun undelete(
    request: OasysGenericRequest,
    oasysAssessmentPk: String,
  ): OasysVersionedEntityResponse {
    val associations = oasysAssociationsService.findDeletedAssociations(oasysAssessmentPk)

    if (associations.isEmpty()) {
      throw NoDeletedAssociationsException(oasysAssessmentPk)
    }

    val response = OasysVersionedEntityResponse()
    val commands = mutableListOf<Command>()
    val onSuccess = mutableListOf<() -> Unit>()

    associations.forEach { association ->
      val entityType = association.entityType ?: throw MissingEntityTypeException(association)

      val versionFrom = association.baseVersion
      val versionTo = oasysAssociationsService
        .findAllIncludingDeleted(association.entityUuid)
        .filter { it.createdAt > association.createdAt }.minByOrNull { it.createdAt }?.baseVersion

      if (versionTo == null) {
        oasysVersionService.findDeletedVersions(association.entityUuid, versionFrom, versionTo)
          .ifEmpty { throw ConflictException("No deleted versions found to undelete for entity ${association.entityUuid}") }

        UndeleteAssessmentCommand(
          assessmentUuid = association.entityUuid,
          user = request.userDetails.toAAPUser(),
          pointInTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(versionFrom), ZoneOffset.UTC),
        ).run(commands::add)
      }

      oasysVersionService.undeleteVersions(association.entityUuid, versionFrom, versionTo)
        .also { response.populate(it, entityType) }

      association.apply { deleted = false }.run(oasysAssociationsService::save)

      if (entityType == EntityType.AAP_PLAN) {
        onSuccess.add {
          oasysEventPublisher.publish(oasysEventFactory.undeleteEvent(association, versionTo))
        }
      }
    }

    commands.ifEmpty { null }?.run(aapApi::dispatchCommands)
    onSuccess.forEach { it() }

    return response
  }

  @Transactional
  fun merge(request: OasysMergeRequest): OasysMessageResponse {
    val notFoundPKs = mutableListOf<String>()
    val conflictPKs = mutableListOf<String>()

    val resultsToMerge = request.merge.associate { merge ->
      oasysAssociationsService.findAssociationsByPk(merge.newOasysAssessmentPK).run {
        if (isNotEmpty()) conflictPKs.add(merge.newOasysAssessmentPK)
      }
      merge.newOasysAssessmentPK to oasysAssociationsService.findAssociationsByPk(merge.oldOasysAssessmentPK, includeDeleted = true)
        .also {
          if (it.isEmpty()) notFoundPKs.add(merge.oldOasysAssessmentPK)
        }
    }

    if (notFoundPKs.isNotEmpty()) {
      throw NoAssociationsException(*notFoundPKs.toTypedArray())
    }

    if (conflictPKs.isNotEmpty()) {
      throw ConflictException("Existing association(s) for ${conflictPKs.joinToString()}")
    }

    val commands = mutableMapOf<UUID, Command>()

    resultsToMerge.map { (newOasysAssessmentPk, existingAssociations) ->
      existingAssociations.map { association ->
        association.apply { oasysAssessmentPk = newOasysAssessmentPk }
          .run(oasysAssociationsService::save)

        commands[association.entityUuid] = UpdateAssessmentPropertiesCommand(
          assessmentUuid = association.entityUuid,
          user = request.userDetails.toAAPUser(),
          added = mapOf("MERGED" to PropertyValue(type = "Single", value = "Y")),
          timeline = Timeline(type = "MERGED"),
        )
      }
    }

    aapApi.dispatchCommands(commands.values.toList())

    log.info(StringUtils.normalizeSpace("Associations transferred by user ID ${request.userDetails.id}: ${request.merge.joinToString { "From ${it.oldOasysAssessmentPK} to ${it.newOasysAssessmentPK}" }}"))

    return OasysMessageResponse("Successfully processed all ${request.merge.size} merge elements")
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

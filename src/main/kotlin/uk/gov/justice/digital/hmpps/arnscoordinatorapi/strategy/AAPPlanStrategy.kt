package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.AAPApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.DailyVersionsQuery
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.TimelineQuery
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.CollectionsView
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.DailyVersionsQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.QueriesResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.SingleValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.TimelineQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.DeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult.Failure
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult.Success
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.ResetData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SoftDeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UndeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetailsList
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.GetPlanResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanState
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.service.OasysVersionService
import java.time.ZoneOffset
import java.util.UUID
import kotlin.getOrElse

@Service
@ConditionalOnProperty(name = ["app.strategies.aap-plan"], havingValue = "true")
class AAPPlanStrategy(
  private val aapApi: AAPApi,
  private val oasysVersionService: OasysVersionService,
  private val clock: Clock,
) : EntityStrategy {

  override val entityType = EntityType.AAP_PLAN

  override fun create(createData: CreateData): OperationResult<VersionedEntity> = createData.plan
    ?.let { createPlanData ->
      aapApi.createAssessment(createPlanData).let { result ->
        when (result) {
          is AAPApi.ApiOperationResult.Failure -> Failure(result.errorMessage)
          is AAPApi.ApiOperationResult.Success -> {
            Success(result.data)
              .also { oasysVersionService.createVersionFor(OasysEvent.CREATED, it.data.id).toOperationResult() }
          }
        }
      }
    }
    ?: Failure("Request did not contain AAP plan data")

  override fun clone(createData: CreateData, entityUuid: UUID): OperationResult<VersionedEntity> = oasysVersionService.createVersionFor(OasysEvent.CLONED, entityUuid).toOperationResult()

  override fun delete(deleteData: DeleteData, entityUuid: UUID): OperationResult<Unit> = when (val result = aapApi.deleteAssessment(entityUuid)) {
    is AAPApi.ApiOperationResult.Success -> Success(Unit)
    is AAPApi.ApiOperationResult.Failure -> Failure(result.errorMessage)
  }

  override fun fetch(entityUuid: UUID): OperationResult<*> = aapApi.fetchAssessment(entityUuid, clock.now())
    .let { apiResponse ->
      when (apiResponse) {
        is AAPApi.ApiOperationResult.Success<AssessmentVersionQueryResult> -> runCatching {
          val version = apiResponse.data.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli()

          val planComplete = apiResponse.data.collections.derivePlanComplete()
          val planType = apiResponse.data.properties.planTypeOrNull()
            ?: return Failure<AssessmentVersionQueryResult>("No value for PLAN_TYPE for entity $entityUuid")

          GetPlanResponse(
            sentencePlanId = entityUuid,
            sentencePlanVersion = version,
            planComplete = planComplete,
            planType = planType,
            lastUpdatedTimestampSP = apiResponse.data.updatedAt,
          )
        }.fold(
          onSuccess = { data -> Success(data) },
          onFailure = { ex ->
            when (ex) {
              is IllegalArgumentException -> Failure("Unable to parse version for entity $entityUuid")
              else -> Failure("Failed to fetch plan for entity $entityUuid")
            }
          },
        )

        is AAPApi.ApiOperationResult.Failure<*> -> Failure<AssessmentVersionQueryResult>(apiResponse.errorMessage)
      }
    }

  override fun fetchVersions(entityUuid: UUID): OperationResult<VersionDetailsList> {
    val oasysVersions = oasysVersionService.fetchAllForEntityUuid(entityUuid).map {
      VersionDetails(
        uuid = it.uuid,
        version = it.version,
        createdAt = it.createdAt,
        updatedAt = it.updatedAt,
        status = it.createdBy.name,
        planAgreementStatus = "",
        entityType = entityType,
      )
    }

    val response = AAPUser(id = "COORDINATOR_API", name = "Coordinator API User")
      .let { user ->
        aapApi.runQueries(
          DailyVersionsQuery(
            user = user,
            assessmentIdentifier = AssessmentIdentifier(entityUuid),
          ),
          TimelineQuery(
            user = user,
            assessmentIdentifier = AssessmentIdentifier(entityUuid),
            pageSize = 99999,
            includeCustomTypes = setOf("PLAN_AGREEMENT_STATUS_CHANGED"),
          ),
        )
      }

    val aapVersions = when (response) {
      is AAPApi.ApiOperationResult.Success<QueriesResponse> -> response.data.queries.flatMap { query ->
        when (query.request) {
          is DailyVersionsQuery -> (query.result as DailyVersionsQueryResult).versions.map {
            VersionDetails(
              uuid = it.lastTimelineItemUuid,
              version = it.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli(),
              createdAt = it.createdAt,
              updatedAt = it.updatedAt,
              status = "UNSIGNED",
              planAgreementStatus = "",
              entityType = EntityType.AAP_PLAN,
            )
          }
          is TimelineQuery -> (query.result as TimelineQueryResult).timeline
            .filter { it.customData?.get("status") == "AGREED" }
            .map { timelineItem ->
              VersionDetails(
                uuid = timelineItem.uuid,
                version = timelineItem.timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                createdAt = timelineItem.timestamp,
                updatedAt = timelineItem.timestamp,
                status = "UNSIGNED",
                planAgreementStatus = "AGREED",
                entityType = EntityType.AAP_PLAN,
              )
            }
          else -> throw IllegalStateException("Unexpected query type: ${query.request::class.simpleName}")
        }
      }
      is AAPApi.ApiOperationResult.Failure<*> -> return Failure<VersionDetailsList>(response.errorMessage)
    }

    return (oasysVersions + aapVersions)
      .groupBy { it.updatedAt }
      .values
      .map { versions ->
        versions.reduce { acc, next ->
          acc.copy(
            status = acc.status.ifBlank { next.status },
            planAgreementStatus = acc.planAgreementStatus?.takeIf { it.isNotBlank() } ?: next.planAgreementStatus,
          )
        }
      }
      .let { Success(it) }
  }

  override fun sign(signData: SignData, entityUuid: UUID): OperationResult<VersionedEntity> = runCatching {
    when (signData.signType) {
      SignType.SELF -> oasysVersionService.createVersionFor(OasysEvent.SELF_SIGNED, entityUuid).toOperationResult()
      SignType.COUNTERSIGN -> oasysVersionService.createVersionFor(OasysEvent.AWAITING_COUNTERSIGN, entityUuid)
        .toOperationResult()
    }
  }.getOrElse {
    Failure("Failed to sign the plan for entity $entityUuid")
  }

  override fun lock(lockData: LockData, entityUuid: UUID): OperationResult<VersionedEntity> = runCatching {
    oasysVersionService.createVersionFor(OasysEvent.LOCKED, entityUuid).toOperationResult()
  }.getOrElse {
    Failure("Failed to lock plan for entity $entityUuid")
  }

  override fun rollback(request: OasysRollbackRequest, entityUuid: UUID): OperationResult<VersionedEntity> = runCatching {
    request.sentencePlanVersionNumber?.let { version ->
      oasysVersionService
        .updateVersion(OasysEvent.ROLLED_BACK, entityUuid, version)
        .toOperationResult()
    } ?: Failure("Plan version does not exist on the request")
  }.fold(
    onSuccess = { it },
    onFailure = {
      Failure("Unable to update version '${request.sentencePlanVersionNumber}' for entity $entityUuid")
    },
  )

  override fun softDelete(softDeleteData: SoftDeleteData, entityUuid: UUID): OperationResult<VersionedEntity?> = runCatching {
    val result =
      oasysVersionService.softDeleteVersions(entityUuid, softDeleteData.versionFrom, softDeleteData.versionTo)

    Success(
      VersionedEntity(
        id = entityUuid,
        version = result.version,
        entityType = EntityType.AAP_PLAN,
      ),
    )
  }.fold(
    onSuccess = { it },
    onFailure = { ex ->
      log.error("Failed to soft-delete versions for entity $entityUuid", ex)
      Failure("Something went wrong while deleting versions for entity $entityUuid")
    },
  )

  override fun undelete(undeleteData: UndeleteData, entityUuid: UUID): OperationResult<VersionedEntity> = runCatching {
    val result = oasysVersionService.undeleteVersions(
      entityUuid,
      undeleteData.versionFrom,
      undeleteData.versionTo,
    )

    Success(
      VersionedEntity(
        id = entityUuid,
        version = result.version,
        entityType = entityType,
      ),
    )
  }.getOrElse { ex ->
    log.error("Failed to undelete versions for entity $entityUuid", ex)
    Failure("Something went wrong while un-deleting versions for entity $entityUuid")
  }

  override fun counterSign(entityUuid: UUID, request: OasysCounterSignRequest): OperationResult<VersionedEntity> = runCatching {
    request.sentencePlanVersionNumber?.let { version ->
      when (request.outcome) {
        CounterSignOutcome.COUNTERSIGNED -> oasysVersionService.updateVersion(
          OasysEvent.COUNTERSIGNED,
          entityUuid,
          version,
        )

        CounterSignOutcome.AWAITING_DOUBLE_COUNTERSIGN -> oasysVersionService.updateVersion(
          OasysEvent.AWAITING_DOUBLE_COUNTERSIGN,
          entityUuid,
          version,
        )

        CounterSignOutcome.DOUBLE_COUNTERSIGNED -> oasysVersionService.updateVersion(
          OasysEvent.DOUBLE_COUNTERSIGNED,
          entityUuid,
          version,
        )

        CounterSignOutcome.REJECTED -> oasysVersionService.updateVersion(OasysEvent.REJECTED, entityUuid, version)
      }.toOperationResult()
    } ?: Failure("Unable to countersign, no plan version number provided for entity $entityUuid")
  }.fold(
    onSuccess = { it },
    onFailure = {
      Failure("Unable to countersign version '${request.sentencePlanVersionNumber}' for entity $entityUuid")
    },
  )

  override fun reset(resetData: ResetData, entityUuid: UUID): OperationResult<VersionedEntity> {
    val user = AAPUser(id = resetData.userDetails.id, name = resetData.userDetails.name)

    return when (val result = aapApi.resetPlan(entityUuid, user)) {
      is AAPApi.ApiOperationResult.Success -> Success(VersionedEntity(id = entityUuid, version = 0, entityType = entityType))
      is AAPApi.ApiOperationResult.Failure -> Failure(result.errorMessage)
    }
  }

  private fun OasysVersionEntity.toOperationResult() = Success(VersionedEntity(entityUuid, version, entityType))

  private fun CollectionsView.derivePlanComplete(): PlanState {
    val latestAgreement = this
      .find { it.name == "PLAN_AGREEMENTS" }
      ?.items
      ?.maxByOrNull { it.updatedAt }
      ?: return PlanState.INCOMPLETE

    val status = (latestAgreement.properties["status"] as? SingleValue)?.value
    return if (status != null && status != "DRAFT") PlanState.COMPLETE else PlanState.INCOMPLETE
  }

  private fun Map<String, Any>.planTypeOrNull(): PlanType? = (this["PLAN_TYPE"] as? SingleValue)
    ?.value
    ?.let(PlanType::valueOf)

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

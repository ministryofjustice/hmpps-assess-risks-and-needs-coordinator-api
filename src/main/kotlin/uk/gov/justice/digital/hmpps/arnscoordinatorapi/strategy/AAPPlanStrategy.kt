package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.AAPApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.SingleValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.DeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult.Failure
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult.Success
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
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["app.strategies.aap-plan"], havingValue = "true")
class AAPPlanStrategy(
  private val aapApi: AAPApi,
  private val oasysVersionService: OasysVersionService,
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

  override fun delete(deleteData: DeleteData, entityUuid: UUID): OperationResult<Unit> = Success(Unit)

  override fun fetch(entityUuid: UUID): OperationResult<*> = aapApi.fetchAssessment(entityUuid, Clock.now())
    .let { result ->
      when (result) {
        is AAPApi.ApiOperationResult.Success<AssessmentVersionQueryResult> -> {
          val version = oasysVersionService.getLatestVersionNumberForEntityUuid(entityUuid)
          try {
            val response = GetPlanResponse(
              sentencePlanId = entityUuid,
              sentencePlanVersion = version,
              planComplete = (result.data.properties.get("PLAN_COMPLETE") as SingleValue?)
                ?.let { PlanState.valueOf(it.value) }
                ?: throw Error("Unable to parse value for PLAN_COMPLETE"),
              planType = (result.data.properties.get("PLAN_TYPE") as SingleValue?)
                ?.let { PlanType.valueOf(it.value) }
                ?: throw Error("Unable to parse value for PLAN_TYPE"),
              lastUpdatedTimestampSP = result.data.updatedAt,
            )
            Success(response)
          } catch (e: Exception) {
            Failure(e.message ?: "Failed to fetch plan for entity $entityUuid")
          }
        }

        is AAPApi.ApiOperationResult.Failure<*> -> Failure(result.errorMessage)
      }
    }

  override fun fetchVersions(entityUuid: UUID): OperationResult<VersionDetailsList> = oasysVersionService.fetchAllForEntityUuid(entityUuid).toVersionDetailsResult()

  override fun sign(signData: SignData, entityUuid: UUID): OperationResult<VersionedEntity> = when (signData.signType) {
    SignType.SELF -> oasysVersionService.createVersionFor(OasysEvent.SELF_SIGNED, entityUuid).toOperationResult()
    SignType.COUNTERSIGN -> oasysVersionService.createVersionFor(OasysEvent.AWAITING_COUNTERSIGN, entityUuid)
      .toOperationResult()
  }

  override fun lock(lockData: LockData, entityUuid: UUID): OperationResult<VersionedEntity> = oasysVersionService.createVersionFor(OasysEvent.LOCKED, entityUuid).toOperationResult()

  override fun rollback(request: OasysRollbackRequest, entityUuid: UUID): OperationResult<VersionedEntity> = request.sentencePlanVersionNumber
    ?.let { version -> oasysVersionService.updateVersion(OasysEvent.ROLLBACK, entityUuid, version) }
    ?.toOperationResult()
    ?: Failure("Unable to find version '${request.sentencePlanVersionNumber}' for entity $entityUuid")

  // TODO: Soft delete from the version table, verify expected behaviour of AAP
  override fun softDelete(softDeleteData: SoftDeleteData, entityUuid: UUID): OperationResult<VersionedEntity?> = oasysVersionService.createVersionFor(OasysEvent.SOFT_DELETE, entityUuid).toOperationResult()

  // TODO: Undelete from the version table, verify expected behaviour of AAP
  override fun undelete(undeleteData: UndeleteData, entityUuid: UUID): OperationResult<VersionedEntity> = oasysVersionService.createVersionFor(OasysEvent.UNDELETE, entityUuid).toOperationResult()

  override fun counterSign(entityUuid: UUID, request: OasysCounterSignRequest): OperationResult<VersionedEntity> = // TODO: Check if we are required to validate the OASys state transition
    request.sentencePlanVersionNumber
      ?.let { version ->
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
        }
      }
      ?.toOperationResult()
      ?: Failure("Unable to find version '${request.sentencePlanVersionNumber}' for entity $entityUuid")
}

private fun OasysVersionEntity.toOperationResult() = Success(VersionedEntity(entityUuid, version, EntityType.AAP_PLAN))

private fun List<OasysVersionEntity>.toVersionDetailsResult(): OperationResult<VersionDetailsList> = this.map { oasysVersion ->
  VersionDetails(
    oasysVersion.entityUuid,
    oasysVersion.version.toInt(),
    createdAt = oasysVersion.createdAt,
    updatedAt = oasysVersion.updatedAt,
    status = oasysVersion.createdBy.name,
    planAgreementStatus = "",
    entityType = EntityType.AAP_PLAN,
  )
}.let { Success(it) }

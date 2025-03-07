package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SoftDeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UndeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.SentencePlanApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CounterSignPlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.PlanVersionData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.SoftDeletePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.UndeletePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.GetPlanResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["app.strategies.plan"], havingValue = "true")
class PlanStrategy(
  private val sentencePlanApi: SentencePlanApi,
) : EntityStrategy {

  override val entityType = EntityType.PLAN

  override fun create(createData: CreateData): OperationResult<VersionedEntity> = when (val result = sentencePlanApi.createPlan(createData.plan!!)) {
    is SentencePlanApi.ApiOperationResult.Failure -> OperationResult.Failure(result.errorMessage)
    is SentencePlanApi.ApiOperationResult.Success -> OperationResult.Success(result.data)
  }

  override fun clone(createData: CreateData, entityUuid: UUID): OperationResult<VersionedEntity> = when (val result = sentencePlanApi.clonePlan(createData.plan!!, entityUuid)) {
    is SentencePlanApi.ApiOperationResult.Failure -> OperationResult.Failure(result.errorMessage)
    is SentencePlanApi.ApiOperationResult.Success -> OperationResult.Success(result.data)
  }

  override fun sign(signData: SignData, entityUuid: UUID): OperationResult<VersionedEntity> = when (val result = sentencePlanApi.signPlan(signData, entityUuid)) {
    is SentencePlanApi.ApiOperationResultExtended.Conflict -> OperationResult.Failure(result.errorMessage, HttpStatus.CONFLICT)
    is SentencePlanApi.ApiOperationResultExtended.Failure -> OperationResult.Failure(result.errorMessage)
    is SentencePlanApi.ApiOperationResultExtended.Success -> OperationResult.Success(result.data)
  }

  override fun lock(lockData: LockData, entityUuid: UUID): OperationResult<VersionedEntity> = when (val result = sentencePlanApi.lockPlan(lockData, entityUuid)) {
    is SentencePlanApi.ApiOperationResultExtended.Conflict -> OperationResult.Failure(result.errorMessage, HttpStatus.CONFLICT)
    is SentencePlanApi.ApiOperationResultExtended.Failure -> OperationResult.Failure(result.errorMessage)
    is SentencePlanApi.ApiOperationResultExtended.Success -> OperationResult.Success(result.data)
  }

  override fun fetch(entityUuid: UUID): OperationResult<GetPlanResponse> = when (val result = sentencePlanApi.getPlan(entityUuid)) {
    is SentencePlanApi.ApiOperationResult.Failure -> OperationResult.Failure(result.errorMessage)
    is SentencePlanApi.ApiOperationResult.Success -> OperationResult.Success(result.data)
  }

  override fun rollback(request: OasysRollbackRequest, entityUuid: UUID): OperationResult<VersionedEntity> = when (val result = sentencePlanApi.rollback(PlanVersionData.from(request), entityUuid)) {
    is SentencePlanApi.ApiOperationResultExtended.Conflict -> OperationResult.Failure(result.errorMessage, HttpStatus.CONFLICT)
    is SentencePlanApi.ApiOperationResultExtended.Failure -> OperationResult.Failure(result.errorMessage)
    is SentencePlanApi.ApiOperationResultExtended.Success -> OperationResult.Success(result.data)
  }

  override fun softDelete(softDeleteData: SoftDeleteData, entityUuid: UUID): OperationResult<VersionedEntity?> = when (val result = sentencePlanApi.softDeletePlan(SoftDeletePlanData.from(softDeleteData), entityUuid)) {
    is SentencePlanApi.ApiOperationResultExtended.Conflict -> OperationResult.Failure(result.errorMessage, HttpStatus.CONFLICT)
    is SentencePlanApi.ApiOperationResultExtended.Failure -> OperationResult.Failure(result.errorMessage)
    is SentencePlanApi.ApiOperationResultExtended.Success -> OperationResult.Success(result.data)
  }
  override fun undelete(undeleteData: UndeleteData, entityUuid: UUID): OperationResult<VersionedEntity> = when (val result = sentencePlanApi.undeletePlan(UndeletePlanData.from(undeleteData), entityUuid)) {
    is SentencePlanApi.ApiOperationResultExtended.Conflict -> OperationResult.Failure(result.errorMessage, HttpStatus.CONFLICT)
    is SentencePlanApi.ApiOperationResultExtended.Failure -> OperationResult.Failure(result.errorMessage)
    is SentencePlanApi.ApiOperationResultExtended.Success -> OperationResult.Success(result.data)
  }

  override fun counterSign(entityUuid: UUID, request: OasysCounterSignRequest): OperationResult<VersionedEntity> = when (val result = sentencePlanApi.counterSign(entityUuid, CounterSignPlanData.from(request))) {
    is SentencePlanApi.ApiOperationResultExtended.Failure -> OperationResult.Failure(result.errorMessage)
    is SentencePlanApi.ApiOperationResultExtended.Conflict -> OperationResult.Failure(result.errorMessage, HttpStatus.CONFLICT)
    is SentencePlanApi.ApiOperationResultExtended.Success -> OperationResult.Success(result.data)
  }
}

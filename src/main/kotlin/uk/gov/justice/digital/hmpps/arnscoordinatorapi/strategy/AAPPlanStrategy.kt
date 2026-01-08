package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.AAPApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.DeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SoftDeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UndeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetailsList
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["app.strategies.aap-plan"], havingValue = "true")
class AAPPlanStrategy(
  private val aapApi: AAPApi,
) : EntityStrategy {

  override val entityType = EntityType.AAP_PLAN

  override fun create(createData: CreateData): OperationResult<VersionedEntity> = when (val result = aapApi.createAssessment(createData.plan!!)) {
    is AAPApi.ApiOperationResult.Failure -> OperationResult.Failure(result.errorMessage)
    is AAPApi.ApiOperationResult.Success -> OperationResult.Success(result.data)
  }

  override fun clone(createData: CreateData, entityUuid: UUID): OperationResult<VersionedEntity> = OperationResult.Failure("AAP Plan clone is not yet implemented")

  override fun delete(deleteData: DeleteData, entityUuid: UUID): OperationResult<Unit> = OperationResult.Failure("AAP Plan delete is not yet implemented")

  override fun fetch(entityUuid: UUID): OperationResult<*> = OperationResult.Failure<Nothing>("AAP Plan fetch is not yet implemented")

  override fun fetchVersions(entityUuid: UUID): OperationResult<VersionDetailsList> = OperationResult.Failure("AAP Plan fetchVersions is not yet implemented")

  override fun sign(signData: SignData, entityUuid: UUID): OperationResult<VersionedEntity> = OperationResult.Failure("AAP Plan sign is not yet implemented")

  override fun lock(lockData: LockData, entityUuid: UUID): OperationResult<VersionedEntity> = OperationResult.Failure("AAP Plan lock is not yet implemented")

  override fun rollback(request: OasysRollbackRequest, entityUuid: UUID): OperationResult<VersionedEntity> = OperationResult.Failure("AAP Plan rollback is not yet implemented")

  override fun softDelete(softDeleteData: SoftDeleteData, entityUuid: UUID): OperationResult<VersionedEntity?> = OperationResult.Failure("AAP Plan softDelete is not yet implemented")

  override fun undelete(undeleteData: UndeleteData, entityUuid: UUID): OperationResult<VersionedEntity> = OperationResult.Failure("AAP Plan undelete is not yet implemented")

  override fun counterSign(entityUuid: UUID, request: OasysCounterSignRequest): OperationResult<VersionedEntity> = OperationResult.Failure("AAP Plan counterSign is not yet implemented")
}

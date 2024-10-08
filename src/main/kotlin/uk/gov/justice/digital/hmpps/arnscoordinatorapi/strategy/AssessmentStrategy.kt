package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.StrengthsAndNeedsApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.GetAssessmentResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["app.strategies.assessment"], havingValue = "true")
class AssessmentStrategy(
  private val strengthsAndNeedsApi: StrengthsAndNeedsApi,
) : EntityStrategy {

  override val entityType = EntityType.ASSESSMENT

  override fun create(createData: CreateData): OperationResult<VersionedEntity> {
    return when (val result = strengthsAndNeedsApi.createAssessment(createData.assessment!!)) {
      is StrengthsAndNeedsApi.ApiOperationResult.Failure -> OperationResult.Failure(result.errorMessage)
      is StrengthsAndNeedsApi.ApiOperationResult.Success -> OperationResult.Success(result.data)
    }
  }

  override fun lock(lockData: LockData, entityUuid: UUID): OperationResult<VersionedEntity> {
    return when (val result = strengthsAndNeedsApi.lockAssessment(lockData, entityUuid)) {
      is StrengthsAndNeedsApi.ApiOperationResultExtended.Failure -> OperationResult.Failure(result.errorMessage)
      is StrengthsAndNeedsApi.ApiOperationResultExtended.Conflict -> OperationResult.Failure(result.errorMessage, HttpStatus.CONFLICT)
      is StrengthsAndNeedsApi.ApiOperationResultExtended.Success -> OperationResult.Success(result.data)
    }
  }

  override fun fetch(entityUuid: UUID): OperationResult<GetAssessmentResponse> {
    return when (val result = strengthsAndNeedsApi.getAssessment(entityUuid)) {
      is StrengthsAndNeedsApi.ApiOperationResult.Failure -> OperationResult.Failure(result.errorMessage)
      is StrengthsAndNeedsApi.ApiOperationResult.Success -> OperationResult.Success(result.data)
    }
  }

  override fun delete(entityUuid: UUID): OperationResult<Unit> {
    return OperationResult.Failure("Delete not implemented yet")
  }
}

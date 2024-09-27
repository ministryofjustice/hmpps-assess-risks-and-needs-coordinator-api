package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.StrengthsAndNeedsApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.IntegrationService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity

@Service
class AssessmentService(
  private val strengthsAndNeedsApi: StrengthsAndNeedsApi,
) : IntegrationService {

  override fun create(createData: CreateData): OperationResult<VersionedEntity> {
    return when (val result = strengthsAndNeedsApi.createAssessment(createData.assessment!!)) {
      is StrengthsAndNeedsApi.ApiOperationResult.Failure -> {
        OperationResult.Failure(result.errorMessage)
      }
      is StrengthsAndNeedsApi.ApiOperationResult.Success -> {
        OperationResult.Success(result.data)
      }
    }
  }
}

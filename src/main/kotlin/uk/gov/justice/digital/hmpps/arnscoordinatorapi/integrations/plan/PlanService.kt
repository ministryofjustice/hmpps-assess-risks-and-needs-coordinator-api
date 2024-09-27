package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.IntegrationService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.SentencePlanApi

@Service
class PlanService(
  private val sentencePlanApi: SentencePlanApi,
) : IntegrationService {

  override fun create(createData: CreateData): OperationResult<VersionedEntity> {
    return when (val result = sentencePlanApi.createPlan(createData.plan!!)) {
      is SentencePlanApi.ApiOperationResult.Failure -> {
        OperationResult.Failure(result.errorMessage)
      }
      is SentencePlanApi.ApiOperationResult.Success -> {
        OperationResult.Success(result.data)
      }
    }
  }
}

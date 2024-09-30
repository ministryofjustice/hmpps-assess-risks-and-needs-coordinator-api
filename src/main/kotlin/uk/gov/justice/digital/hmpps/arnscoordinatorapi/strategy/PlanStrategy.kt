package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.SentencePlanApi
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["app.strategies.plan"], havingValue = "true", matchIfMissing = true)
class PlanStrategy(
  private val sentencePlanApi: SentencePlanApi,
) : EntityStrategy {

  override val entityType = EntityType.PLAN

  override fun create(createData: CreateData): OperationResult<VersionedEntity> {
    return when (val result = sentencePlanApi.createPlan(createData.plan!!)) {
      is SentencePlanApi.ApiOperationResult.Failure -> OperationResult.Failure(result.errorMessage)
      is SentencePlanApi.ApiOperationResult.Success -> OperationResult.Success(result.data)
    }
  }

  override fun delete(entityUuid: UUID): OperationResult<Unit> {
    TODO("Not yet implemented")
  }
}

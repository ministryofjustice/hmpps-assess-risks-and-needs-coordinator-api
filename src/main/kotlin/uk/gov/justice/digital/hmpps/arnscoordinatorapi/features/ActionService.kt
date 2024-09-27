package uk.gov.justice.digital.hmpps.arnscoordinatorapi.features

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.IntegrationService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity

@Service
class ActionService(
  private val components: List<IntegrationService>,
) {

  fun createAllEntities(createData: CreateData): OperationResult<List<VersionedEntity>> {
    val successResponses = mutableListOf<VersionedEntity>()
    val failureResponses = mutableListOf<String>()

    components.forEach { component ->
      when (val createResult = component.create(createData)) {
        is OperationResult.Success -> successResponses.add(createResult.data)
        is OperationResult.Failure -> failureResponses.add(createResult.errorMessage)
      }
    }

    return if (failureResponses.isNotEmpty()) {
      OperationResult.Failure(failureResponses.joinToString())
    } else {
      OperationResult.Success(successResponses)
    }
  }
}

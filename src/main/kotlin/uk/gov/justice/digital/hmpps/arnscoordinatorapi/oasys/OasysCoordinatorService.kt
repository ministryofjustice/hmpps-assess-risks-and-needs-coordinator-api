package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.features.ActionService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysCreateResponse

@Service
class OasysCoordinatorService(
  private val actionService: ActionService,
  private val oasysAssociationsService: OasysAssociationsService,
) {

  private fun buildCreateData(requestData: OasysCreateRequest): CreateData {
    return CreateData(
      plan = CreatePlanData(
        planType = requestData.planType,
        userDetails = UserDetails(
          id = requestData.userDetails.id,
          name = requestData.userDetails.name,
        ),
      ),
      assessment = CreateAssessmentData(
        userDetails = UserDetails(
          id = requestData.userDetails.id,
          name = requestData.userDetails.name,
        ),
      ),
    )
  }

  fun create(requestData: OasysCreateRequest): CreateOperationResult<OasysCreateResponse> {
    oasysAssociationsService.ensureNoExistingAssociation(requestData.oasysAssessmentPk)
      .onFailure { return CreateOperationResult.ConflictingAssociations("Cannot create due to conflicting associations: $it") }

    when (val createAllResult = actionService.createAllEntities(buildCreateData(requestData))) {
      is OperationResult.Failure -> return CreateOperationResult.Failure("Cannot create, creating entities failed: ${createAllResult.errorMessage}")
      is OperationResult.Success -> {
        val oasysCreateResponse = OasysCreateResponse()

        for (versionedEntity in createAllResult.data) {
          oasysCreateResponse.addVersionedEntity(versionedEntity)

          oasysAssociationsService.storeAssociation(
            OasysAssociation(
              oasysAssessmentPk = requestData.oasysAssessmentPk,
              regionPrisonCode = requestData.regionPrisonCode,
              entityUuid = versionedEntity.id,
              entityType = versionedEntity.entityType,
            ),
          ).onFailure {
            // TODO: Probably want to do some kind of rollback here in future
            return CreateOperationResult.Failure("Failed saving associations: $it")
          }
        }

        return CreateOperationResult.Success(oasysCreateResponse)
      }
    }
  }

  sealed class CreateOperationResult<out T> {
    data class Success<T>(val data: T) : CreateOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : CreateOperationResult<T>()

    data class ConflictingAssociations<T>(
      val errorMessage: String,
    ) : CreateOperationResult<T>()
  }
}

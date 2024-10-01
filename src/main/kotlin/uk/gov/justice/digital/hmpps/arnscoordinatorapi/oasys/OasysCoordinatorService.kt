package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.Command
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.CommandFactory
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysCreateResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.StrategyFactory

@Service
class OasysCoordinatorService(
  private val strategyFactory: StrategyFactory,
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

  @Transactional
  fun create(requestData: OasysCreateRequest): CreateOperationResult<OasysCreateResponse> {
    oasysAssociationsService.ensureNoExistingAssociation(requestData.oasysAssessmentPk)
      .onFailure { return CreateOperationResult.ConflictingAssociations("Cannot create due to conflicting associations: $it") }

    val oasysCreateResponse = OasysCreateResponse()
    val successfullyExecutedCommands: MutableList<CreateCommand> = mutableListOf()

    for (strategy in strategyFactory.getStrategies()) {
      val command = CreateCommand(strategy, buildCreateData(requestData))
      val commandResult = command.execute()

      when (commandResult) {
        is OperationResult.Success -> successfullyExecutedCommands.add(command)
        is OperationResult.Failure -> {
          successfullyExecutedCommands.forEach { it.rollback() }
          return CreateOperationResult.Failure("Failed to create entity for ${strategy.entityType}: ${commandResult.errorMessage}")
        }
      }

      when (
        oasysAssociationsService.storeAssociation(
          OasysAssociation(
            oasysAssessmentPk = requestData.oasysAssessmentPk,
            regionPrisonCode = requestData.regionPrisonCode,
            entityType = strategy.entityType,
            entityUuid = commandResult.data.id,
          ),
        )
      ) {
        is OperationResult.Success -> oasysCreateResponse.addVersionedEntity(commandResult.data)
        is OperationResult.Failure -> {
          successfullyExecutedCommands.forEach { priorCommand -> priorCommand.rollback() }
          return CreateOperationResult.Failure("Failed saving association for ${strategy.entityType}")
        }
      }
    }

    return CreateOperationResult.Success(oasysCreateResponse)
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

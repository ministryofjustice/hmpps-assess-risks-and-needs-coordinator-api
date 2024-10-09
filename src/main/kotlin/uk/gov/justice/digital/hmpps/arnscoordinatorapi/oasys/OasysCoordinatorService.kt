package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.CreateCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.FetchCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.LockCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.RollbackCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysAssociationsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
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
          type = UserType.OASYS,
        ),
      ),
    )
  }

  @Transactional
  fun create(requestData: OasysCreateRequest): CreateOperationResult<OasysVersionedEntityResponse> {
    oasysAssociationsService.ensureNoExistingAssociation(requestData.oasysAssessmentPk)
      .onFailure { return CreateOperationResult.ConflictingAssociations("Cannot create due to conflicting associations: $it") }

    val oasysCreateResponse = OasysVersionedEntityResponse()
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

  @Transactional
  fun lock(oasysGenericRequest: OasysGenericRequest, oasysAssessmentPk: String): LockOperationResult<OasysVersionedEntityResponse> {
    val associations = oasysAssociationsService.findAssociations(oasysAssessmentPk)

    if (associations.isEmpty()) {
      return LockOperationResult.NoAssociations("No associations found for the provided OASys Assessment PK")
    }

    val oasysLockResponse = OasysVersionedEntityResponse()
    for (association in associations) {
      val strategy = association.entityType?.let(strategyFactory::getStrategy)
        ?: return LockOperationResult.Failure("Strategy not initialized for ${association.entityType}")

      val command = LockCommand(strategy, association.entityUuid!!, LockData(UserDetails(oasysGenericRequest.userDetails.id, oasysGenericRequest.userDetails.name)))

      when (val response = command.execute()) {
        is OperationResult.Failure -> {
          if (response.statusCode === HttpStatus.CONFLICT) {
            return LockOperationResult.Conflict("Failed to lock ${association.entityType} entity due to a conflict, ${response.errorMessage}")
          }

          return LockOperationResult.Failure("Failed to lock ${association.entityType} entity, ${response.errorMessage}")
        }
        is OperationResult.Success -> oasysLockResponse.addVersionedEntity(response.data)
      }
    }

    return LockOperationResult.Success(oasysLockResponse)
  }

  @Transactional
  fun rollback(rollbackRequest: OasysRollbackRequest, oasysAssessmentPk: String): RollbackOperationResult<OasysVersionedEntityResponse> {
    val associations = oasysAssociationsService.findAssociations(oasysAssessmentPk)

    if (associations.isEmpty()) {
      return RollbackOperationResult.NoAssociations("No associations found for the provided OASys Assessment PK")
    }

    val oasysRollbackResponse = OasysVersionedEntityResponse()
    for (association in associations) {
      val strategy = association.entityType?.let(strategyFactory::getStrategy)
        ?: return RollbackOperationResult.Failure("Strategy not initialized for ${association.entityType}")

      val command = RollbackCommand(strategy, association.entityUuid!!, rollbackRequest)

      when (val response = command.execute()) {
        is OperationResult.Failure -> {
          if (response.statusCode === HttpStatus.CONFLICT) {
            return RollbackOperationResult.Conflict("Failed to rollback ${association.entityType} entity due to a conflict, ${response.errorMessage}")
          }

          return RollbackOperationResult.Failure("Failed to rollback ${association.entityType} entity, ${response.errorMessage}")
        }
        is OperationResult.Success -> oasysRollbackResponse.addVersionedEntity(response.data)
      }
    }

    return RollbackOperationResult.Success(oasysRollbackResponse)
  }

  fun get(oasysAssessmentPk: String): GetOperationResult<OasysGetResponse> {
    val associations = oasysAssociationsService.findAssociations(oasysAssessmentPk)

    if (associations.isEmpty()) {
      return GetOperationResult.NoAssociations("No associations found for the provided OASys Assessment PK")
    }

    val oasysGetResponse = OasysGetResponse()
    for (association in associations) {
      val strategy = association.entityType?.let(strategyFactory::getStrategy)
        ?: return GetOperationResult.Failure("Strategy not initialized for ${association.entityType}")

      val command = FetchCommand(strategy, association.entityUuid!!)

      when (val response = command.execute()) {
        is OperationResult.Failure -> return GetOperationResult.Failure("Failed to retrieve ${association.entityType} entity, ${response.errorMessage}")
        is OperationResult.Success -> oasysGetResponse.addEntityData(response.data!!)
      }
    }

    return GetOperationResult.Success(oasysGetResponse)
  }

  fun getAssociations(oasysAssessmentPk: String): GetOperationResult<OasysAssociationsResponse> {
    val associations = oasysAssociationsService.findAssociations(oasysAssessmentPk)

    if (associations.isEmpty()) {
      return GetOperationResult.NoAssociations("No associations found for the provided OASys Assessment PK")
    }

    val oasysAssociationsResponse = OasysAssociationsResponse()
    associations.forEach {
      when (it.entityType) {
        EntityType.ASSESSMENT -> oasysAssociationsResponse.apply {
          sanAssessmentId = it.entityUuid
        }
        EntityType.PLAN -> oasysAssociationsResponse.apply {
          sentencePlanId = it.entityUuid
        }
        null -> return GetOperationResult.Failure("Misconfigured association found")
      }
    }

    return GetOperationResult.Success(oasysAssociationsResponse)
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

  sealed class GetOperationResult<out T> {
    data class Success<T>(val data: T) : GetOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : GetOperationResult<T>()

    data class NoAssociations<T>(
      val errorMessage: String,
    ) : GetOperationResult<T>()
  }

  sealed class LockOperationResult<out T> {
    data class Success<T>(val data: T) : LockOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : LockOperationResult<T>()

    data class NoAssociations<T>(
      val errorMessage: String,
    ) : LockOperationResult<T>()

    data class Conflict<T>(
      val errorMessage: String,
    ) : LockOperationResult<T>()
  }

  sealed class RollbackOperationResult<out T> {
    data class Success<T>(val data: T) : RollbackOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : RollbackOperationResult<T>()

    data class NoAssociations<T>(
      val errorMessage: String,
    ) : RollbackOperationResult<T>()

    data class Conflict<T>(
      val errorMessage: String,
    ) : RollbackOperationResult<T>()
  }
}

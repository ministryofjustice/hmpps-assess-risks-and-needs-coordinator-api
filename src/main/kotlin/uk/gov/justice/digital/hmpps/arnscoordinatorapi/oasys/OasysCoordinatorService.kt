package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.CounterSignCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.CreateCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.FetchCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.LockCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.RollbackCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.SignCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysSignRequest
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
        userDetails = requestData.userDetails.intoUserDetails(),
      ),
      assessment = CreateAssessmentData(
        userDetails = requestData.userDetails.intoUserDetails(),
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
        OasysAssociation(
          oasysAssessmentPk = requestData.oasysAssessmentPk,
          regionPrisonCode = requestData.regionPrisonCode,
          entityType = strategy.entityType,
          entityUuid = commandResult.data.id,
        ).run(oasysAssociationsService::storeAssociation)
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
  fun associateWithPrevious(requestData: OasysCreateRequest): CreateOperationResult<OasysVersionedEntityResponse> {
    oasysAssociationsService.ensureNoExistingAssociation(requestData.oasysAssessmentPk)
      .onFailure { return CreateOperationResult.ConflictingAssociations("Cannot create due to conflicting associations: $it") }

    return when (val previous = requestData.previousOasysAssessmentPk?.run(::get)) {
      is GetOperationResult.Success -> with(previous.data) {
        strategyFactory.getStrategies().map { strategy ->
          OasysAssociation(
            entityType = strategy.entityType,
            entityUuid = idFor(strategy.entityType),
            baseVersion = versionFor(strategy.entityType),
            oasysAssessmentPk = requestData.oasysAssessmentPk,
            regionPrisonCode = requestData.regionPrisonCode,
          ).run(oasysAssociationsService::storeAssociation)
            .onFailure { return CreateOperationResult.Failure("Failed to store association") }
        }
        CreateOperationResult.Success(previous.data)
      }

      is GetOperationResult.NoAssociations -> CreateOperationResult.Failure("No associations found for the provided OASys Assessment PK: ${requestData.previousOasysAssessmentPk}")
      else -> CreateOperationResult.Failure("Failed to get previous versions for OASys Assessment PK: ${requestData.previousOasysAssessmentPk}")
    }
  }

  @Transactional
  fun lock(
    oasysGenericRequest: OasysGenericRequest,
    oasysAssessmentPk: String,
  ): LockOperationResult<OasysVersionedEntityResponse> {
    val associations = oasysAssociationsService.findAssociations(oasysAssessmentPk)

    if (associations.isEmpty()) {
      return LockOperationResult.NoAssociations("No associations found for the provided OASys Assessment PK")
    }

    val oasysLockResponse = OasysVersionedEntityResponse()
    for (association in associations) {
      val strategy = association.entityType?.let(strategyFactory::getStrategy)
        ?: return LockOperationResult.Failure("Strategy not initialized for ${association.entityType}")

      val command = LockCommand(strategy, association.entityUuid!!, LockData(oasysGenericRequest.userDetails.intoUserDetails()))

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
  fun sign(
    oasysSignRequest: OasysSignRequest,
    oasysAssessmentPk: String,
  ): SignOperationResult<OasysVersionedEntityResponse> {
    val associations = oasysAssociationsService.findAssociations(oasysAssessmentPk)

    if (associations.isEmpty()) {
      return SignOperationResult.NoAssociations("No associations found for the provided OASys Assessment PK")
    }

    val oasysSignResponse = OasysVersionedEntityResponse()
    for (association in associations) {
      val strategy = association.entityType?.let(strategyFactory::getStrategy)
        ?: return SignOperationResult.Failure("Strategy not initialized for ${association.entityType}")

      val command = SignCommand(
        strategy,
        association.entityUuid!!,
        SignData(
          signType = oasysSignRequest.signType,
          userDetails = oasysSignRequest.userDetails.intoUserDetails(),
        ),
      )

      when (val response = command.execute()) {
        is OperationResult.Failure -> {
          if (response.statusCode === HttpStatus.CONFLICT) {
            return SignOperationResult.Conflict("Failed to sign ${association.entityType} entity due to a conflict, ${response.errorMessage}")
          }

          return SignOperationResult.Failure("Failed to sign ${association.entityType} entity, ${response.errorMessage}")
        }

        is OperationResult.Success -> oasysSignResponse.addVersionedEntity(response.data)
      }
    }

    return SignOperationResult.Success(oasysSignResponse)
  }

  @Transactional
  fun rollback(
    rollbackRequest: OasysRollbackRequest,
    oasysAssessmentPk: String,
  ): RollbackOperationResult<OasysVersionedEntityResponse> {
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
            return RollbackOperationResult.Conflict("Failed to roll back ${association.entityType} entity due to a conflict, ${response.errorMessage}")
          }

          return RollbackOperationResult.Failure("Failed to roll back ${association.entityType} entity, ${response.errorMessage}")
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

  fun counterSign(
    oasysAssessmentPk: String,
    request: OasysCounterSignRequest,
  ): CounterSignOperationResult<OasysVersionedEntityResponse> {
    val associations = oasysAssociationsService.findAssociations(oasysAssessmentPk)

    if (associations.isEmpty()) {
      return CounterSignOperationResult.NoAssociations("No associations found for the provided OASys Assessment PK")
    }

    val response = OasysVersionedEntityResponse()
    associations.forEach { association ->
      val strategy = association.entityType?.run(strategyFactory::getStrategy)
        ?: return CounterSignOperationResult.Failure("Strategy not initialized for ${association.entityType}")

      val command = association.entityUuid?.let { uuid ->
        CounterSignCommand(
          strategy,
          uuid,
          request,
        )
      } ?: return CounterSignOperationResult.Failure("No entity UUID for association ${association.uuid}")

      when (val result = command.execute()) {
        is OperationResult.Failure -> {
          if (result.statusCode === HttpStatus.CONFLICT) {
            return CounterSignOperationResult.Conflict("Failed to countersign ${association.entityType} entity due to a conflict")
          }

          return CounterSignOperationResult.Failure("Failed to countersign ${association.entityType} with UUID ${association.uuid}")
        }

        is OperationResult.Success -> response.addVersionedEntity(result.data)
      }
    }

    return CounterSignOperationResult.Success(response)
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

  sealed class CounterSignOperationResult<out T> {
    data class Success<T>(val data: T) : CounterSignOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : CounterSignOperationResult<T>()

    data class NoAssociations<T>(
      val errorMessage: String,
    ) : CounterSignOperationResult<T>()

    data class Conflict<T>(
      val errorMessage: String,
    ) : CounterSignOperationResult<T>()
  }

  sealed class SignOperationResult<out T> {
    data class Success<T>(val data: T) : SignOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : SignOperationResult<T>()

    data class NoAssociations<T>(
      val errorMessage: String,
    ) : SignOperationResult<T>()

    data class Conflict<T>(
      val errorMessage: String,
    ) : SignOperationResult<T>()
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

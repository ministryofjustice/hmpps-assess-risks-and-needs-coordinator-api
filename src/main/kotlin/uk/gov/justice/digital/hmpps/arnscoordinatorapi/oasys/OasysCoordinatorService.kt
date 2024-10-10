package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
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
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserType
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
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.StrategyFactory
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class OasysCoordinatorService(
  private val strategyFactory: StrategyFactory,
  private val oasysAssociationsService: OasysAssociationsService,
  private val taskExecutor: ThreadPoolTaskExecutor,
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

    val commandResponses = strategyFactory.getStrategies().runAsync(
      data = buildCreateData(requestData),
      taskExecutor = taskExecutor,
      executeCommand = { args: List<Any> ->
        val command = CreateCommand(args[0] as EntityStrategy, args[1] as CreateData)
        val result = command.execute()
        Pair(result, command)
      },
    )

    for ((commandResult, strategy, command) in commandResponses) {
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

    val commandResponses = associations.runAsync(
      data = LockData(UserDetails(oasysGenericRequest.userDetails.id, oasysGenericRequest.userDetails.name)),
      strategyFactory = strategyFactory,
      taskExecutor = taskExecutor,
      executeCommand = { args: List<Any> ->
        val command = LockCommand(args[0] as EntityStrategy, args[1] as UUID, args[2] as LockData)
        val result = command.execute()
        Pair(result, command)
      },
    )

    for ((response, association) in commandResponses) {
      when (response) {
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
  fun sign(oasysSignRequest: OasysSignRequest, oasysAssessmentPk: String): SignOperationResult<OasysVersionedEntityResponse> {
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
          userDetails = UserDetails(
            oasysSignRequest.userDetails.id,
            oasysSignRequest.userDetails.name,
          ),
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
    val commandResponses = associations.runAsync(
      strategyFactory = strategyFactory,
      taskExecutor = taskExecutor,
      executeCommand = { args: List<Any> ->
        val command = FetchCommand(args[0] as EntityStrategy, args[1] as UUID)
        val result = command.execute()
        Pair(result, command)
      },
    )

    for ((response, association) in commandResponses) {
      when (response) {
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

inline fun <reified T, reified C> List<OasysAssociation>.runAsync(
  taskExecutor: ThreadPoolTaskExecutor,
  strategyFactory: StrategyFactory,
  data: Any? = null,
  crossinline executeCommand: (args: List<Any>) -> Pair<OperationResult<T>, C>,
) =
  mapNotNull { assoc ->
    CompletableFuture.supplyAsync(
      {
        assoc.entityType?.let(strategyFactory::getStrategy)?.let {
          val res = executeCommand(listOfNotNull(it, assoc.entityUuid, data))
          Triple(res.first, assoc, res.second)
        } ?: Triple(OperationResult.Failure("Strategy not initialized for ${assoc.entityType}"), assoc, null)
      },
      taskExecutor,
    )
  }.map { it.join() }

inline fun <reified T, reified C> List<EntityStrategy>.runAsync(
  taskExecutor: ThreadPoolTaskExecutor,
  data: Any? = null,
  crossinline executeCommand: (args: List<Any>) -> Pair<OperationResult<T>, C>,
) =
  mapNotNull { strategy ->
    CompletableFuture.supplyAsync(
      {
        val res = executeCommand(listOfNotNull(strategy, data))
        Triple(res.first, strategy, res.second)
      },
      taskExecutor,
    )
  }.map { it.join() }

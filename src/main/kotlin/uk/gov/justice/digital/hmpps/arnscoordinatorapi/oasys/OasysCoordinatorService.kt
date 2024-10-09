package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.CreateCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.FetchCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.LockCommand
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

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.CloneCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.CounterSignCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.CreateCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.FetchCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.LockCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.RollbackCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.SignCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.SoftDeleteCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.UndeleteCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SoftDeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UndeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.OasysAssociationsService
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysMergeRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysAssociationsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysMessageResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.StrategyFactory
import java.util.UUID

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

  fun clone(requestData: OasysCreateRequest): CreateOperationResult<OasysVersionedEntityResponse> {
    oasysAssociationsService.ensureNoExistingAssociation(requestData.oasysAssessmentPk)
      .onFailure { return CreateOperationResult.ConflictingAssociations("An association already exists for the provided OASys Assessment PK: ${requestData.oasysAssessmentPk}, $it.") }

    val associations = oasysAssociationsService.findAssociations(requestData.previousOasysAssessmentPk!!)
    if (associations.isEmpty()) {
      return CreateOperationResult.NoAssociations("No associations found for the provided OASys Assessment PK")
    }

    val oasysCreateResponse = OasysVersionedEntityResponse()
    val successfullyExecutedCommands: MutableList<CloneCommand> = mutableListOf()

    associations.forEach { association ->
      val command = CloneCommand(strategyFactory.getStrategy(association.entityType!!), buildCreateData(requestData), association.entityUuid)

      when (val commandResult = command.execute()) {
        is OperationResult.Success -> {
          successfullyExecutedCommands.add(command)

          OasysAssociation(
            entityType = commandResult.data.entityType,
            entityUuid = commandResult.data.id,
            baseVersion = commandResult.data.version,
            oasysAssessmentPk = requestData.oasysAssessmentPk,
            regionPrisonCode = requestData.regionPrisonCode,
          ).run(oasysAssociationsService::storeAssociation)
            .onFailure {
              return CreateOperationResult.Failure("Failed to store association")
            }

          oasysCreateResponse.addVersionedEntity(commandResult.data)
        }
        is OperationResult.Failure -> {
          successfullyExecutedCommands.forEach { it.rollback() }
          return CreateOperationResult.Failure("Failed to clone entity for ${association.entityType}: ${commandResult.errorMessage}")
        }
      }
    }

    return CreateOperationResult.Success(oasysCreateResponse)
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

      val command = LockCommand(strategy, association.entityUuid, LockData(oasysGenericRequest.userDetails.intoUserDetails()))

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
        association.entityUuid,
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

      val command = RollbackCommand(strategy, association.entityUuid, rollbackRequest)

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

      val command = FetchCommand(strategy, association.entityUuid)

      when (val response = command.execute()) {
        is OperationResult.Failure -> return GetOperationResult.Failure("Failed to retrieve ${association.entityType} entity, ${response.errorMessage}")
        is OperationResult.Success -> oasysGetResponse.addEntityData(response.data!!)
      }
    }

    return GetOperationResult.Success(oasysGetResponse)
  }

  fun getByEntityId(entityUuid: UUID, entityType: EntityType): GetOperationResult<OasysGetResponse> {
    val oasysAssessmentPk = oasysAssociationsService.findOasysPkByEntityId(entityUuid)
      ?: return GetOperationResult.NoAssociations("No associations found for the provided entityUuid")
    val association = oasysAssociationsService.findAssociations(oasysAssessmentPk)
      .firstOrNull { it.entityType == entityType }
      ?: return GetOperationResult.NoAssociations("No associations found for the provided entityUuid and entityType")

    val oasysGetResponse = OasysGetResponse()

    val strategy = association.entityType?.let(strategyFactory::getStrategy)
      ?: return GetOperationResult.Failure("Strategy not initialized for ${association.entityType}")
    val command = FetchCommand(strategy, association.entityUuid)

    when (val response = command.execute()) {
      is OperationResult.Failure -> return GetOperationResult.Failure("Failed to retrieve $entityType entity, ${response.errorMessage}")
      is OperationResult.Success -> oasysGetResponse.addEntityData(response.data!!)
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

      val command = CounterSignCommand(
        strategy,
        association.entityUuid,
        request,
      )

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

  @Transactional
  fun softDelete(
    oasysGenericRequest: OasysGenericRequest,
    oasysAssessmentPk: String,
  ): SoftDeleteOperationResult<OasysVersionedEntityResponse> {
    val associations = oasysAssociationsService.findAssociations(oasysAssessmentPk)

    if (associations.isEmpty()) {
      return SoftDeleteOperationResult.NoAssociations("No associations found for the provided OASys Assessment PK")
    }

    val oasysSoftDeleteResponse = OasysVersionedEntityResponse()
    for (association in associations) {
      val strategy = association.entityType?.let(strategyFactory::getStrategy)
        ?: return SoftDeleteOperationResult.Failure("Strategy not initialized for ${association.entityType}")

      val versionTo = oasysAssociationsService
        .findAllIncludingDeleted(association.entityUuid)
        .filter { it.createdAt > association.createdAt }
        .sortedBy { it.createdAt }
        .firstOrNull()?.baseVersion

      val command = SoftDeleteCommand(
        strategy,
        association.entityUuid,
        SoftDeleteData(
          oasysGenericRequest.userDetails.intoUserDetails(),
          association.baseVersion,
          versionTo,
        ),
      )

      when (val response = command.execute()) {
        is OperationResult.Failure -> {
          if (response.statusCode === HttpStatus.CONFLICT) {
            return SoftDeleteOperationResult.Conflict("Failed to soft-delete ${association.entityType} versions from ${association.baseVersion} to $versionTo due to a conflict, ${response.errorMessage}")
          }

          return SoftDeleteOperationResult.Failure("Failed to soft-delete association for ${association.oasysAssessmentPk}, ${response.errorMessage}")
        }

        is OperationResult.Success -> {
          when (association.apply { deleted = true }.run(oasysAssociationsService::storeAssociation)) {
            is OperationResult.Success -> response.data?.run(oasysSoftDeleteResponse::addVersionedEntity)
            is OperationResult.Failure -> {
              return SoftDeleteOperationResult.Failure("Failed setting the association for ${strategy.entityType} to deleted")
            }
          }
        }
      }
    }

    return SoftDeleteOperationResult.Success(oasysSoftDeleteResponse)
  }

  @Transactional
  fun undelete(
    oasysGenericRequest: OasysGenericRequest,
    oasysAssessmentPk: String,
  ): UndeleteOperationResult<OasysVersionedEntityResponse> {
    val associations = oasysAssociationsService.findDeletedAssociations(oasysAssessmentPk)

    if (associations.isEmpty()) {
      return UndeleteOperationResult.NoAssociations("No deleted associations found for the provided OASys Assessment PK")
    }

    val oasysUndeleteResponse = OasysVersionedEntityResponse()
    for (association in associations) {
      val strategy = association.entityType?.let(strategyFactory::getStrategy)
        ?: return UndeleteOperationResult.Failure("Strategy not initialized for ${association.entityType}")

      val versionTo = oasysAssociationsService
        .findAllIncludingDeleted(association.entityUuid)
        .filter { it.createdAt > association.createdAt }.minByOrNull { it.createdAt }?.baseVersion

      val command = UndeleteCommand(
        strategy,
        association.entityUuid,
        UndeleteData(
          oasysGenericRequest.userDetails.intoUserDetails(),
          association.baseVersion,
          versionTo,
        ),
      )

      when (val response = command.execute()) {
        is OperationResult.Failure -> {
          if (response.statusCode === HttpStatus.CONFLICT) {
            return UndeleteOperationResult.Conflict("Failed to undelete ${association.entityType} versions from ${association.baseVersion} to $versionTo due to a conflict, ${response.errorMessage}")
          }

          return UndeleteOperationResult.Failure("Failed to undelete association for ${association.oasysAssessmentPk}, ${response.errorMessage}")
        }

        is OperationResult.Success -> {
          when (association.apply { deleted = false }.run(oasysAssociationsService::storeAssociation)) {
            is OperationResult.Success -> oasysUndeleteResponse.addVersionedEntity(response.data)
            is OperationResult.Failure -> {
              return UndeleteOperationResult.Failure("Failed undeleting the association for ${strategy.entityType}")
            }
          }
        }
      }
    }

    return UndeleteOperationResult.Success(oasysUndeleteResponse)
  }

  @Transactional
  fun merge(request: OasysMergeRequest): MergeOperationResult<OasysMessageResponse> {
    val notFoundPKs = mutableListOf<String>()
    val conflictPKs = mutableListOf<String>()

    val oldAssociations = request.merge.map { merge ->
      oasysAssociationsService.findAssociations(merge.newOasysAssessmentPK).run {
        if (isNotEmpty()) conflictPKs.add(merge.newOasysAssessmentPK)
      }
      merge.newOasysAssessmentPK to oasysAssociationsService.findAssociations(merge.oldOasysAssessmentPK)
        .also {
          if (it.isEmpty()) notFoundPKs.add(merge.oldOasysAssessmentPK)
        }
    }.toMap()

    if (notFoundPKs.isNotEmpty()) {
      return MergeOperationResult.NoAssociations("The following association(s) could not be located: ${notFoundPKs.joinToString()} and the operation has not been actioned.")
    }

    if (conflictPKs.isNotEmpty()) {
      return MergeOperationResult.Conflict("Existing association(s) for ${conflictPKs.joinToString()}")
    }

    oldAssociations.map {
      it.value.map { association ->
        association.apply { oasysAssessmentPk = it.key }
          .run(oasysAssociationsService::storeAssociation)
          .onFailure { error ->
            return MergeOperationResult.Failure("Failed to store ${association.entityType?.name} association ${association.uuid}")
          }
      }
    }

    log.info("Associations transferred by user ID ${request.userDetails.id}: ${request.merge.map { "From ${it.oldOasysAssessmentPK} to ${it.newOasysAssessmentPK}" }.joinToString()}")

    return MergeOperationResult.Success(OasysMessageResponse("Successfully processed all ${request.merge.size} merge elements"))
  }

  sealed class CreateOperationResult<out T> {
    data class Success<T>(val data: T) : CreateOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : CreateOperationResult<T>()

    data class NoAssociations<T>(
      val errorMessage: String,
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

  sealed class SoftDeleteOperationResult<out T> {
    data class Success<T>(val data: T) : SoftDeleteOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : SoftDeleteOperationResult<T>()

    data class NoAssociations<T>(
      val errorMessage: String,
    ) : SoftDeleteOperationResult<T>()

    data class Conflict<T>(
      val errorMessage: String,
    ) : SoftDeleteOperationResult<T>()
  }

  sealed class UndeleteOperationResult<out T> {
    data class Success<T>(val data: T) : UndeleteOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : UndeleteOperationResult<T>()

    data class NoAssociations<T>(
      val errorMessage: String,
    ) : UndeleteOperationResult<T>()

    data class Conflict<T>(
      val errorMessage: String,
    ) : UndeleteOperationResult<T>()
  }

  sealed class MergeOperationResult<out T> {
    data class Success<T>(val data: T) : MergeOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : MergeOperationResult<T>()

    data class NoAssociations<T>(
      val errorMessage: String,
    ) : MergeOperationResult<T>()

    data class Conflict<T>(
      val errorMessage: String,
    ) : MergeOperationResult<T>()
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

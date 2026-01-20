package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionAspectSupport
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.CounterSignCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.CreateCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.FetchCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.FetchVersionsCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.LockCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.RollbackCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.SignCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.SoftDeleteCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands.UndeleteCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.VersionsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.VersionsResponseFactory
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.ResetData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SoftDeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UndeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
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

  private fun buildCreateData(requestData: OasysCreateRequest): CreateData = CreateData(
    plan = CreatePlanData(
      planType = requestData.planType,
      userDetails = requestData.userDetails.intoUserDetails(),
      subjectDetails = requestData.subjectDetails,
    ),
    assessment = CreateAssessmentData(
      userDetails = requestData.userDetails.intoUserDetails(),
    ),
  )

  private fun linkExistingEntity(
    previousPk: String,
    newPk: String,
    entityType: EntityType,
    regionPrisonCode: String?,
  ): EntityResult {
    val existingAssociation = oasysAssociationsService
      .findAssociationsByPkAndType(previousPk, listOf(entityType))
      .firstOrNull()
      ?: return EntityResult.NotFound("No $entityType association found for PK $previousPk")

    val newAssociation = OasysAssociation(
      oasysAssessmentPk = newPk,
      entityType = entityType,
      entityUuid = existingAssociation.entityUuid,
      baseVersion = existingAssociation.baseVersion,
      regionPrisonCode = regionPrisonCode,
    )

    return when (oasysAssociationsService.storeAssociation(newAssociation)) {
      is OperationResult.Success -> EntityResult.Success(
        VersionedEntity(
          id = existingAssociation.entityUuid,
          version = existingAssociation.baseVersion,
          entityType = entityType,
        ),
      )
      is OperationResult.Failure -> EntityResult.Failure("Failed to store $entityType association")
    }
  }

  private fun createNewEntity(
    request: OasysCreateRequest,
    entityType: EntityType,
    successfulCommands: MutableList<CreateCommand>,
  ): EntityResult {
    val strategy = strategyFactory.getStrategy(entityType)
    val command = CreateCommand(strategy, buildCreateData(request))

    return when (val result = command.execute()) {
      is OperationResult.Success -> {
        successfulCommands.add(command)

        val association = OasysAssociation(
          oasysAssessmentPk = request.oasysAssessmentPk,
          entityType = entityType,
          entityUuid = result.data.id,
          regionPrisonCode = request.regionPrisonCode,
        )

        when (oasysAssociationsService.storeAssociation(association)) {
          is OperationResult.Success -> EntityResult.Success(result.data)
          is OperationResult.Failure -> EntityResult.Failure("Failed to store $entityType association")
        }
      }
      is OperationResult.Failure -> EntityResult.Failure("Failed to create $entityType: ${result.errorMessage}")
    }
  }

  private fun handleSanEntity(
    request: OasysCreateRequest,
    successfulCommands: MutableList<CreateCommand>,
  ): EntityResult = if (request.previousOasysSanPk != null) {
    linkExistingEntity(
      previousPk = request.previousOasysSanPk,
      newPk = request.oasysAssessmentPk,
      entityType = EntityType.ASSESSMENT,
      regionPrisonCode = request.regionPrisonCode,
    )
  } else {
    createNewEntity(
      request = request,
      entityType = EntityType.ASSESSMENT,
      successfulCommands = successfulCommands,
    )
  }

  private fun handleSpEntity(
    request: OasysCreateRequest,
    successfulCommands: MutableList<CreateCommand>,
  ): EntityResult {
    val spEntityType = EntityType.AAP_PLAN

    return if (request.previousOasysSpPk != null) {
      val linkResult = linkExistingEntity(
        previousPk = request.previousOasysSpPk,
        newPk = request.oasysAssessmentPk,
        entityType = spEntityType,
        regionPrisonCode = request.regionPrisonCode,
      )

      if (linkResult is EntityResult.Success && request.newPeriodOfSupervision) {
        val resetResult = resetLinkedEntity(
          entityUuid = linkResult.entity.id,
          entityType = spEntityType,
          userDetails = request.userDetails.intoUserDetails(),
        )

        if (resetResult is EntityResult.Failure) {
          return resetResult
        }
      }

      linkResult
    } else {
      createNewEntity(
        request = request,
        entityType = spEntityType,
        successfulCommands = successfulCommands,
      )
    }
  }

  private fun resetLinkedEntity(
    entityUuid: UUID,
    entityType: EntityType,
    userDetails: uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails,
  ): EntityResult {
    val strategy = strategyFactory.getStrategy(entityType)
    val resetData = ResetData(userDetails = userDetails)

    return when (val result = strategy.reset(resetData, entityUuid)) {
      is OperationResult.Success -> EntityResult.Success(result.data)
      is OperationResult.Failure -> EntityResult.Failure("Failed to reset $entityType: ${result.errorMessage}")
    }
  }

  @Transactional
  fun create(requestData: OasysCreateRequest): CreateOperationResult<OasysVersionedEntityResponse> {
    oasysAssociationsService.ensureNoExistingAssociation(requestData.oasysAssessmentPk)
      .onFailure { return CreateOperationResult.ConflictingAssociations("Cannot create due to conflicting associations: $it") }

    val response = OasysVersionedEntityResponse()
    val successfulCommands: MutableList<CreateCommand> = mutableListOf()

    // Handle SAN (Assessment) - only if assessmentType includes it
    if (EntityType.ASSESSMENT in requestData.assessmentType.entityTypes) {
      when (val sanResult = handleSanEntity(requestData, successfulCommands)) {
        is EntityResult.Success -> response.addVersionedEntity(sanResult.entity)
        is EntityResult.Failure -> {
          successfulCommands.forEach { it.rollback() }
          TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
          return CreateOperationResult.Failure(sanResult.message)
        }
        is EntityResult.NotFound -> {
          successfulCommands.forEach { it.rollback() }
          TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
          return CreateOperationResult.NoAssociations(sanResult.message)
        }
      }
    }

    // Handle SP (Plan) - always included
    when (val spResult = handleSpEntity(requestData, successfulCommands)) {
      is EntityResult.Success -> response.addVersionedEntity(spResult.entity)
      is EntityResult.Failure -> {
        successfulCommands.forEach { it.rollback() }
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
        return CreateOperationResult.Failure(spResult.message)
      }
      is EntityResult.NotFound -> {
        successfulCommands.forEach { it.rollback() }
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
        return CreateOperationResult.NoAssociations(spResult.message)
      }
    }

    return CreateOperationResult.Success(response)
  }

  @Transactional
  fun lock(
    oasysGenericRequest: OasysGenericRequest,
    oasysAssessmentPk: String,
  ): LockOperationResult<OasysVersionedEntityResponse> {
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

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
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

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
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

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
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

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
    val association = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)
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

  fun getVersionsByEntityId(entityUuid: UUID, authType: String?): GetOperationResult<VersionsResponse> {
    val oasysAssessmentPk = oasysAssociationsService.findOasysPkByEntityId(entityUuid)
      ?: return GetOperationResult.NoAssociations("No associations found for the provided entityUuid")

    val versionsResponseFactory = VersionsResponseFactory()

    val associations = if (authType == "HMPPS_AUTH") {
      oasysAssociationsService.findAssociationsByPkAndType(oasysAssessmentPk, listOf(EntityType.PLAN))
    } else {
      oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)
    }

    for (association in associations) {
      val strategy = association.entityType?.let(strategyFactory::getStrategy)
        ?: return GetOperationResult.Failure("Strategy not initialized for ${association.entityType}")

      val command = FetchVersionsCommand(strategy, association.entityUuid)

      when (val response = command.execute()) {
        is OperationResult.Failure -> return GetOperationResult.Failure("Failed to retrieve ${association.entityType} entity versions, ${response.errorMessage}")
        is OperationResult.Success -> versionsResponseFactory.addVersions(response.data)
      }
    }

    return GetOperationResult.Success(versionsResponseFactory.getVersionsResponse())
  }

  fun getAssociations(oasysAssessmentPk: String): GetOperationResult<OasysAssociationsResponse> {
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

    if (associations.isEmpty()) {
      return GetOperationResult.NoAssociations("No associations found for the provided OASys Assessment PK")
    }

    val oasysAssociationsResponse = OasysAssociationsResponse()
    associations.forEach {
      when (it.entityType) {
        EntityType.ASSESSMENT -> oasysAssociationsResponse.apply {
          sanAssessmentId = it.entityUuid
        }

        EntityType.PLAN, EntityType.AAP_PLAN -> oasysAssociationsResponse.apply {
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
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

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
    val associations = oasysAssociationsService.findAssociationsByPk(oasysAssessmentPk)

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
        .minByOrNull { it.createdAt }?.baseVersion

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

    val resultsToMerge = request.merge.associate { merge ->
      oasysAssociationsService.findAssociationsByPk(merge.newOasysAssessmentPK).run {
        if (isNotEmpty()) conflictPKs.add(merge.newOasysAssessmentPK)
      }
      merge.newOasysAssessmentPK to oasysAssociationsService.findAssociationsByPk(merge.oldOasysAssessmentPK, includeDeleted = true)
        .also {
          if (it.isEmpty()) notFoundPKs.add(merge.oldOasysAssessmentPK)
        }
    }

    if (notFoundPKs.isNotEmpty()) {
      return MergeOperationResult.NoAssociations("The following association(s) could not be located: ${notFoundPKs.joinToString()} and the operation has not been actioned.")
    }

    if (conflictPKs.isNotEmpty()) {
      return MergeOperationResult.Conflict("Existing association(s) for ${conflictPKs.joinToString()}")
    }

    resultsToMerge.map { (newOasysAssessmentPk, existingAssociations) ->
      existingAssociations.map { association ->
        association.apply { oasysAssessmentPk = newOasysAssessmentPk }
          .run(oasysAssociationsService::storeAssociation)
          .onFailure {
            return MergeOperationResult.Failure("Failed to store ${association.entityType?.name} association ${association.uuid}")
          }
      }
    }

    log.info(StringUtils.normalizeSpace("Associations transferred by user ID ${request.userDetails.id}: ${request.merge.joinToString { "From ${it.oldOasysAssessmentPK} to ${it.newOasysAssessmentPK}" }}"))

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

  private sealed class EntityResult {
    data class Success(val entity: VersionedEntity) : EntityResult()
    data class Failure(val message: String) : EntityResult()
    data class NotFound(val message: String) : EntityResult()
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

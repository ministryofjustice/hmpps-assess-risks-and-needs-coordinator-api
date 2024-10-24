package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CounterSignPlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.PlanVersionData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.UndeletePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.CreatePlanResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.GetPlanResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.LockPlanResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.PlanVersionResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["app.strategies.plan"], havingValue = "true")
class SentencePlanApi(
  val sentencePlanApiWebClient: WebClient,
  val apiProperties: SentencePlanApiProperties,
) {

  fun createPlan(createData: CreatePlanData): ApiOperationResult<VersionedEntity> {
    return try {
      val result = sentencePlanApiWebClient.post()
        .uri(apiProperties.endpoints.create)
        .body(BodyInserters.fromValue(createData))
        .retrieve()
        .bodyToMono(CreatePlanResponse::class.java)
        .map {
          VersionedEntity(
            id = it.sentencePlanId,
            version = it.sentencePlanVersion,
            entityType = EntityType.PLAN,
          )
        }
        .block()

      result?.let {
        ApiOperationResult.Success(it)
      } ?: throw IllegalStateException("Unexpected error during createPlan")
    } catch (ex: WebClientResponseException) {
      ApiOperationResult.Failure("HTTP error during create plan: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}", ex)
    } catch (ex: Exception) {
      ApiOperationResult.Failure("Unexpected error during createPlan: ${ex.message}", ex)
    }
  }

  fun clonePlan(createData: CreatePlanData, planUuid: UUID): ApiOperationResult<VersionedEntity> {
    return try {
      val result = sentencePlanApiWebClient.post()
        .uri(apiProperties.endpoints.clone.replace("{uuid}", planUuid.toString()))
        .body(BodyInserters.fromValue(createData))
        .retrieve()
        .bodyToMono(CreatePlanResponse::class.java)
        .map {
          VersionedEntity(
            id = it.sentencePlanId,
            version = it.sentencePlanVersion,
            entityType = EntityType.PLAN,
          )
        }
        .block()

      result?.let {
        ApiOperationResult.Success(it)
      } ?: throw IllegalStateException("Unexpected error during clonePlan")
    } catch (ex: WebClientResponseException) {
      ApiOperationResult.Failure("HTTP error during clone plan: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}", ex)
    } catch (ex: Exception) {
      ApiOperationResult.Failure("Unexpected error during clonePlan: ${ex.message}", ex)
    }
  }

  fun signPlan(signData: SignData, planUuid: UUID): ApiOperationResultExtended<VersionedEntity> {
    return try {
      val result = sentencePlanApiWebClient.post()
        .uri(apiProperties.endpoints.sign.replace("{uuid}", planUuid.toString()))
        .body(BodyInserters.fromValue(signData))
        .retrieve()
        .bodyToMono(PlanVersionResponse::class.java)
        .map {
          VersionedEntity(
            id = it.planId,
            version = it.planVersion,
            entityType = EntityType.PLAN,
          )
        }
        .block()

      result?.let {
        ApiOperationResultExtended.Success(it)
      } ?: throw IllegalStateException("Unexpected error during signPlan")
    } catch (ex: WebClientResponseException) {
      if (ex.statusCode.value() == HttpStatus.CONFLICT.value()) {
        return ApiOperationResultExtended.Conflict("Sentence Plan is already signed")
      }
      ApiOperationResultExtended.Failure("HTTP error during sign plan: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}")
    } catch (ex: Exception) {
      ApiOperationResultExtended.Failure("Unexpected error during signPlan: ${ex.message}", ex)
    }
  }

  fun lockPlan(lockData: LockData, planUuid: UUID): ApiOperationResultExtended<VersionedEntity> {
    return try {
      val result = sentencePlanApiWebClient.post()
        .uri(apiProperties.endpoints.lock.replace("{uuid}", planUuid.toString()))
        .body(BodyInserters.fromValue(lockData))
        .retrieve()
        .bodyToMono(LockPlanResponse::class.java)
        .map {
          VersionedEntity(
            id = it.sentencePlanId,
            version = it.sentencePlanVersion,
            entityType = EntityType.PLAN,
          )
        }
        .block()

      result?.let {
        ApiOperationResultExtended.Success(it)
      } ?: throw IllegalStateException("Unexpected error during lockPlan")
    } catch (ex: WebClientResponseException) {
      if (ex.statusCode.value() == HttpStatus.CONFLICT.value()) {
        return ApiOperationResultExtended.Conflict("Sentence Plan is already locked")
      }
      ApiOperationResultExtended.Failure("HTTP error during lock plan: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}")
    } catch (ex: Exception) {
      ApiOperationResultExtended.Failure("Unexpected error during lockPlan: ${ex.message}", ex)
    }
  }

  fun getPlan(planUuid: UUID): ApiOperationResult<GetPlanResponse> {
    return try {
      val result = sentencePlanApiWebClient.get()
        .uri("${apiProperties.endpoints.fetch}/$planUuid")
        .retrieve()
        .bodyToMono(GetPlanResponse::class.java)
        .block()

      result?.let {
        ApiOperationResult.Success(it)
      } ?: throw IllegalStateException("Unexpected null response during getPlan")
    } catch (ex: WebClientResponseException) {
      ApiOperationResult.Failure("HTTP error during get plan: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}", ex)
    } catch (ex: Exception) {
      ApiOperationResult.Failure("Unexpected error during getPlan: ${ex.message}", ex)
    }
  }

  fun rollback(data: PlanVersionData, planUuid: UUID): ApiOperationResultExtended<VersionedEntity> {
    return try {
      val result = sentencePlanApiWebClient.post()
        .uri(apiProperties.endpoints.rollback.replace("{uuid}", planUuid.toString()))
        .body(BodyInserters.fromValue(data))
        .retrieve()
        .bodyToMono(PlanVersionResponse::class.java)
        .map {
          VersionedEntity(
            id = it.planId,
            version = it.planVersion,
            entityType = EntityType.PLAN,
          )
        }
        .block()

      result?.let {
        ApiOperationResultExtended.Success(it)
      } ?: throw IllegalStateException("Unexpected error during rollbackPlan")
    } catch (ex: WebClientResponseException) {
      if (ex.statusCode.value() == HttpStatus.CONFLICT.value()) {
        return ApiOperationResultExtended.Conflict("Unable to roll back this plan version")
      }
      ApiOperationResultExtended.Failure("HTTP error during rollback plan version: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}")
    } catch (ex: Exception) {
      ApiOperationResultExtended.Failure("Unexpected error during rollbackPlanVersion: ${ex.message}", ex)
    }
  }

  fun counterSign(assessmentUuid: UUID, data: CounterSignPlanData): ApiOperationResultExtended<VersionedEntity> {
    return try {
      val result = sentencePlanApiWebClient.post()
        .uri(apiProperties.endpoints.counterSign.replace("{uuid}", assessmentUuid.toString()))
        .body(BodyInserters.fromValue(data))
        .retrieve()
        .bodyToMono(PlanVersionResponse::class.java)
        .map {
          VersionedEntity(
            id = it.planId,
            version = it.planVersion,
            entityType = EntityType.PLAN,
          )
        }
        .block()

      result?.let {
        ApiOperationResultExtended.Success(it)
      } ?: throw IllegalStateException("Unexpected error during counterSign")
    } catch (ex: WebClientResponseException) {
      if (ex.statusCode.value() == HttpStatus.CONFLICT.value()) {
        return ApiOperationResultExtended.Conflict("Sentence Plan could not be counter-signed, Response body: ${ex.responseBodyAsString}")
      }
      ApiOperationResultExtended.Failure("HTTP error during counterSign plan: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}")
    } catch (ex: Exception) {
      ApiOperationResultExtended.Failure("Unexpected error during counterSign: ${ex.message}", ex)
    }
  }

  fun undeletePlan(undeleteData: UndeletePlanData, planUuid: UUID): ApiOperationResultExtended<VersionedEntity> {
    return try {
      val result = sentencePlanApiWebClient.post()
        .uri(apiProperties.endpoints.undelete.replace("{uuid}", planUuid.toString()))
        .body(BodyInserters.fromValue(undeleteData))
        .retrieve()
        .bodyToMono(PlanVersionResponse::class.java)
        .map {
          VersionedEntity(
            id = it.planId,
            version = it.planVersion,
            entityType = EntityType.PLAN,
          )
        }
        .block()

      result?.let {
        ApiOperationResultExtended.Success(it)
      } ?: throw IllegalStateException("Unexpected error during undelete plan version")
    } catch (ex: WebClientResponseException) {
      if (ex.statusCode.value() == HttpStatus.CONFLICT.value()) {
        return ApiOperationResultExtended.Conflict("Unable to undelete the requested sentence plan versions: ${ex.responseBodyAsString}")
      }
      ApiOperationResultExtended.Failure("HTTP error during undelete plan version: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}")
    } catch (ex: Exception) {
      ApiOperationResultExtended.Failure("Unexpected error during undeletePlanVersion: ${ex.message}", ex)
    }
  }

  sealed class ApiOperationResult<out T> {
    data class Success<T>(val data: T) : ApiOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : ApiOperationResult<T>() {
      init {
        LoggerFactory.getLogger(SentencePlanApi::class.java).error(errorMessage)
      }
    }
  }

  sealed class ApiOperationResultExtended<out T> {
    data class Success<T>(val data: T) : ApiOperationResultExtended<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : ApiOperationResultExtended<T>() {
      init {
        LoggerFactory.getLogger(SentencePlanApi::class.java).error(errorMessage)
      }
    }

    data class Conflict<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : ApiOperationResultExtended<T>() {
      init {
        LoggerFactory.getLogger(SentencePlanApi::class.java).error(errorMessage)
      }
    }
  }
}

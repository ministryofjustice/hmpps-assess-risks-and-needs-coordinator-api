package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api

import org.slf4j.LoggerFactory
<<<<<<< HEAD
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
=======
import org.springframework.http.HttpStatus
>>>>>>> 34c20e2 (SP2-791: Added lock assessment and plan)
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.exception.AlreadyLockedException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.CreatePlanResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.GetPlanResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.LockPlanResponse
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

  fun lockPlan(lockData: LockData, planUuid: UUID): ApiOperationResult<VersionedEntity> {
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
        ApiOperationResult.Success(it)
      } ?: throw IllegalStateException("Unexpected error during lockPlan")
    } catch (ex: WebClientResponseException) {
      if (ex.statusCode.value() == HttpStatus.CONFLICT.value()) {
        throw AlreadyLockedException("Sentence Plan is already locked")
      }
      ApiOperationResult.Failure("HTTP error during lock plan: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}", ex)
    } catch (ex: Exception) {
      ApiOperationResult.Failure("Unexpected error during lockPlan: ${ex.message}", ex)
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
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.CreatePlanResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType

@Component
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

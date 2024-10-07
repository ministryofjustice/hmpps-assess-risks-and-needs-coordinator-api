package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.CreateAssessmentResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.GetAssessmentResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["app.strategies.assessment"], havingValue = "true", matchIfMissing = true)
class StrengthsAndNeedsApi(
  val sanApiWebClient: WebClient,
  val apiProperties: StrengthsAndNeedsApiProperties,
) {

  fun createAssessment(createData: CreateAssessmentData): ApiOperationResult<VersionedEntity> {
    return try {
      val result = sanApiWebClient.post()
        .uri(apiProperties.endpoints.create)
        .body(BodyInserters.fromValue(createData))
        .retrieve()
        .bodyToMono(CreateAssessmentResponse::class.java)
        .map {
          VersionedEntity(
            id = it.sanAssessmentId,
            version = it.sanAssessmentVersion,
            entityType = EntityType.ASSESSMENT,
          )
        }
        .block()

      result?.let {
        ApiOperationResult.Success(it)
      } ?: throw IllegalStateException("Unexpected error during createAssessment")
    } catch (ex: WebClientResponseException) {
      ApiOperationResult.Failure("HTTP error during create assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}", ex)
    } catch (ex: Exception) {
      ApiOperationResult.Failure("Unexpected error during createAssessment: ${ex.message}", ex)
    }
  }

  fun getAssessment(assessmentUuid: UUID): ApiOperationResult<GetAssessmentResponse> {
    return try {
      val result = sanApiWebClient.get()
        .uri("${apiProperties.endpoints.fetch}/$assessmentUuid")
        .retrieve()
        .bodyToMono(GetAssessmentResponse::class.java)
        .block()

      result?.let {
        ApiOperationResult.Success(it)
      } ?: throw IllegalStateException("Unexpected null response during getAssessment")
    } catch (ex: WebClientResponseException) {
      ApiOperationResult.Failure("HTTP error during get assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}", ex)
    } catch (ex: Exception) {
      ApiOperationResult.Failure("Unexpected error during getAssessment: ${ex.message}", ex)
    }
  }

  sealed class ApiOperationResult<out T> {
    data class Success<T>(val data: T) : ApiOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : ApiOperationResult<T>() {
      init {
        LoggerFactory.getLogger(StrengthsAndNeedsApi::class.java).error(errorMessage)
      }
    }
  }
}

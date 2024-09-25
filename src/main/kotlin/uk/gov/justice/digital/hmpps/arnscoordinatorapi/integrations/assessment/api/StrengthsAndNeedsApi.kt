package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.CreateAssessmentResponse

@Component
class StrengthsAndNeedsApi(
  val sanApiWebClient: WebClient,
  val apiProperties: StrengthsAndNeedsApiProperties,
) {

  private val logger = LoggerFactory.getLogger(StrengthsAndNeedsApi::class.java)

  fun createAssessment(body: CreateAssessmentRequest): CreateAssessmentResponse? {
    return try {
      sanApiWebClient.post()
        .uri(apiProperties.endpoints.create)
        .body(BodyInserters.fromValue(body))
        .retrieve()
        .bodyToMono(CreateAssessmentResponse::class.java)
        .block()
    } catch (ex: WebClientResponseException) {
      logger.error("HTTP error during createAssessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}", ex)
      throw ex
    } catch (ex: Exception) {
      logger.error("Error during createAssessment: ${ex.message}", ex)
      throw ex
    }
  }
}

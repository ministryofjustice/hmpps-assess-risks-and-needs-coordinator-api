package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["app.strategies.aap-plan"], havingValue = "true")
class AAPApi(
  val aapApiWebClient: WebClient,
  val apiProperties: AAPApiProperties,
) {

  fun createAssessment(createData: CreatePlanData): ApiOperationResult<VersionedEntity> = try {
    val command = CreateAssessmentCommand(
      assessmentType = "SENTENCE_PLAN",
      formVersion = "", // note: we leave this empty and then set it when the user gets into AAPxSP
      user = AAPUser(id = createData.userDetails.id, name = createData.userDetails.name),
    )

    val result = aapApiWebClient.post()
      .uri(apiProperties.endpoints.command)
      .body(BodyInserters.fromValue(command))
      .retrieve()
      .bodyToMono(CreateAssessmentCommandResult::class.java)
      .map {
        VersionedEntity(
          id = UUID.fromString(it.assessmentUuid),
          version = 0,
          entityType = EntityType.AAP_PLAN,
        )
      }
      .block()

    result?.let {
      ApiOperationResult.Success(it)
    } ?: throw IllegalStateException("Unexpected error during createAssessment")
  } catch (ex: WebClientResponseException) {
    ApiOperationResult.Failure("HTTP error during create AAP assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}", ex)
  } catch (ex: Exception) {
    ApiOperationResult.Failure("Unexpected error during createAssessment: ${ex.message}", ex)
  }

  sealed class ApiOperationResult<out T> {
    data class Success<T>(val data: T) : ApiOperationResult<T>()

    data class Failure<T>(
      val errorMessage: String,
      val cause: Throwable? = null,
    ) : ApiOperationResult<T>() {
      init {
        LoggerFactory.getLogger(AAPApi::class.java).error(errorMessage)
      }
    }
  }
}

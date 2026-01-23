package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.IdentifierType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.QueriesRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.time.LocalDateTime
import java.util.UUID

@Component
@ConditionalOnProperty(name = ["app.strategies.aap-plan"], havingValue = "true")
class AAPApi(
  val aapApiWebClient: WebClient,
  val apiProperties: AAPApiProperties,
) {

  fun createAssessment(createData: CreatePlanData): ApiOperationResult<VersionedEntity> = try {
    val identifiers = buildIdentifiers(createData)

    val command = CreateAssessmentCommand(
      assessmentType = "SENTENCE_PLAN",
      formVersion = "", // note: we leave this empty and then set it when the user gets into AAPxSP
      identifiers = identifiers,
      user = AAPUser(id = createData.userDetails.id, name = createData.userDetails.name),
    )

    val request = CommandsRequest.of(command)

    val response = aapApiWebClient.post()
      .uri(apiProperties.endpoints.command)
      .body(BodyInserters.fromValue(request))
      .retrieve()
      .bodyToMono(CommandsResponse::class.java)
      .block()

    response?.commands?.firstOrNull()?.result.let {
      when (it) {
        is CreateAssessmentCommandResult -> ApiOperationResult.Success(
          VersionedEntity(
            id = it.assessmentUuid,
            version = 0,
            entityType = EntityType.AAP_PLAN,
          ),
        )
        null -> throw IllegalStateException("No command result returned from AAP API")
        else -> throw IllegalStateException("Unexpected command result type: ${it::class.simpleName}")
      }
    }
  } catch (ex: WebClientResponseException) {
    ApiOperationResult.Failure(
      "HTTP error during create AAP assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}",
      ex,
    )
  } catch (ex: Exception) {
    ApiOperationResult.Failure("Unexpected error during createAssessment: ${ex.message}", ex)
  }

  fun fetchAssessment(entityUuid: UUID, timestamp: LocalDateTime): ApiOperationResult<AssessmentVersionQueryResult> = try {
    AssessmentVersionQuery(
      user = AAPUser(id = "COORDINATOR_API", name = "Coordinator API User"),
      assessmentIdentifier = AssessmentIdentifier(entityUuid),
      timestamp = timestamp,
    )
      .let { query ->
        aapApiWebClient.post()
          .uri(apiProperties.endpoints.query)
          .body(BodyInserters.fromValue(QueriesRequest.of(query)))
          .retrieve()
          .bodyToMono(AssessmentVersionQueryResult::class.java)
          .block()
      }
      ?.let { result -> ApiOperationResult.Success(result) }
      ?: throw IllegalStateException("Unexpected error during fetchAssessment")
  } catch (ex: WebClientResponseException) {
    ApiOperationResult.Failure(
      "HTTP error during fetch AAP assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}",
      ex,
    )
  } catch (ex: Exception) {
    ApiOperationResult.Failure("Unexpected error during fetchAssessment: ${ex.message}", ex)
  }

  private fun buildIdentifiers(createData: CreatePlanData): Map<IdentifierType, String>? {
    val identifiers = mutableMapOf<IdentifierType, String>()

    createData.subjectDetails?.crn?.let { identifiers[IdentifierType.CRN] = it }
    createData.subjectDetails?.nomisId?.let { identifiers[IdentifierType.NOMIS_ID] = it }

    return identifiers.ifEmpty { null }
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

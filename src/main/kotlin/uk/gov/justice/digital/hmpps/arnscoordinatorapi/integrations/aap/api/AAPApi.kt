package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.CommandsRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.IdentifierType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.PropertyValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.QueriesRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.Query
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command.CommandsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.QueriesResponse
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

    val properties = mapOf(
      "PLAN_TYPE" to PropertyValue(type = "Single", value = createData.planType.name),
    )

    val command = CreateAssessmentCommand(
      assessmentType = "SENTENCE_PLAN",
      formVersion = "", // note: we leave this empty and then set it when the user gets into AAPxSP
      properties = properties,
      identifiers = identifiers,
      flags = createData.flags,
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

  fun deleteAssessment(assessmentUuid: UUID): ApiOperationResult<Unit> = try {
    aapApiWebClient.delete()
      .uri(apiProperties.endpoints.delete.replace("{uuid}", assessmentUuid.toString()))
      .retrieve()
      .bodyToMono(Void::class.java)
      .block()

    ApiOperationResult.Success(Unit)
  } catch (ex: WebClientResponseException) {
    ApiOperationResult.Failure(
      "HTTP error during delete AAP assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}",
      ex,
    )
  } catch (ex: Exception) {
    ApiOperationResult.Failure("Unexpected error during deleteAssessment: ${ex.message}", ex)
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
          .bodyToMono(QueriesResponse::class.java)
          .block()
      }
      ?.queries?.firstOrNull()?.result
      ?.let { result -> ApiOperationResult.Success(result as AssessmentVersionQueryResult) }
      ?: throw IllegalStateException("No query result returned from AAP API")
  } catch (ex: WebClientResponseException) {
    ApiOperationResult.Failure(
      "HTTP error during fetch AAP assessment: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}",
      ex,
    )
  } catch (ex: Exception) {
    ApiOperationResult.Failure("Unexpected error during fetchAssessment: ${ex.message}", ex)
  }

  fun runQueries(vararg queries: Query): ApiOperationResult<QueriesResponse> = try {
    aapApiWebClient.post()
      .uri(apiProperties.endpoints.query)
      .body(BodyInserters.fromValue(QueriesRequest(queries.toList())))
      .retrieve()
      .bodyToMono(QueriesResponse::class.java)
      .block()
      ?.let { result -> ApiOperationResult.Success(result) }
      ?: throw IllegalStateException("No query result returned from AAP API")
  } catch (ex: WebClientResponseException) {
    ApiOperationResult.Failure(
      "HTTP error during fetch AAP versions: Status code ${ex.statusCode}, Response body: ${ex.responseBodyAsString}",
      ex,
    )
  } catch (ex: Exception) {
    ApiOperationResult.Failure("Unexpected error during fetchVersions: ${ex.message}", ex)
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

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.ResetPlanRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.Command
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.CommandsRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.QueriesRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.Query
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command.CommandsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.QueriesResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.QueryResult
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@Component
class AAPApi(
  @param:Qualifier("aapApiWebClient")
  private val aapApiWebClient: WebClient,
) {
  fun dispatchCommands(commands: List<Command>): CommandsResponse = aapApiWebClient
    .post()
    .uri("/command")
    .bodyValue(CommandsRequest(commands))
    .retrieve()
    .onStatus(
      { it.isError },
      {
        it.bodyToMono(ErrorResponse::class.java)
          .map { res -> IllegalStateException("Call to Assessment Platform API failed with status ${res.status}: ${res.developerMessage}") }
      },
    )
    .bodyToMono(CommandsResponse::class.java)
    .block()
    ?: throw RuntimeException("Empty response from Assessment Platform API")

  fun dispatchQueries(queries: List<Query>): QueriesResponse = aapApiWebClient
    .post()
    .uri("/query")
    .bodyValue(QueriesRequest(queries))
    .retrieve()
    .onStatus(
      { it.isError },
      {
        it.bodyToMono(ErrorResponse::class.java)
          .map { res -> IllegalStateException("Call to Assessment Platform API failed with status ${res.status}: ${res.developerMessage}") }
      },
    )
    .bodyToMono(QueriesResponse::class.java)
    .block()
    ?: throw RuntimeException("Empty response from Assessment Platform API")

  fun dispatchQueries(queries: List<Pair<Query, (QueryResult) -> Unit>>) {
    val response = dispatchQueries(queries.map { (query, _) -> query })
    queries.forEachIndexed { index, (_, closure) ->
      closure(response.queries[index].result)
    }
  }

  fun deleteAssessment(assessmentUuid: UUID) = aapApiWebClient
    .delete()
    .uri("/assessment/$assessmentUuid")
    .retrieve()
    .toBodilessEntity()
    .block()

  fun resetPlan(request: ResetPlanRequest) = aapApiWebClient
    .post()
    .uri("/plan/start-new-period-of-supervision")
    .body(BodyInserters.fromValue(request))
    .retrieve()
    .bodyToMono(Void::class.java)
    .block()
}

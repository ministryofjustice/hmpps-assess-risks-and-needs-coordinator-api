package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.CoordinatorException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.Command
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.CommandsRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.QueriesRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.Query
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command.CommandsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.QueriesResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.QueryResult
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

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
    .onStatus({ it.isError }, ::toCoordinatorException)
    .bodyToMono(CommandsResponse::class.java)
    .block()
    ?: throw CoordinatorException("Empty response from Assessment Platform API")

  fun dispatchQueries(queries: List<Query>): QueriesResponse = aapApiWebClient
    .post()
    .uri("/query")
    .bodyValue(QueriesRequest(queries))
    .retrieve()
    .onStatus({ it.isError }, ::toCoordinatorException)
    .bodyToMono(QueriesResponse::class.java)
    .block()
    ?: throw CoordinatorException("Empty response from Assessment Platform API")

  fun dispatchQueries(queries: List<Pair<Query, (QueryResult) -> Unit>>) {
    val response = dispatchQueries(queries.map { (query, _) -> query })
    queries.forEachIndexed { index, (_, closure) ->
      closure(response.queries[index].result)
    }
  }

  private fun toCoordinatorException(response: ClientResponse): Mono<Throwable> = response
    .bodyToMono(ErrorResponse::class.java)
    .map { it.developerMessage ?: it.userMessage.orEmpty() }
    .onErrorResume { Mono.empty() }
    .defaultIfEmpty("")
    .map { detail ->
      val message = "Call to Assessment Platform API failed with status ${response.statusCode().value()}: $detail"
      when (response.statusCode().value()) {
        HttpStatus.CONFLICT.value() -> ConflictException(message)
        else -> CoordinatorException(message = message, developerMessage = message)
      }
    }
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.webclient.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.io.IOException
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${app.services.hmpps-auth.base-url}") val hmppsAuthBaseUri: String,
  @param:Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @param:Value("\${api.timeout:5s}") val timeout: Duration,
  @param:Value("\${api.retry.max-retries:2}") val retryMaxRetries: Long,
  @param:Value("\${api.retry.min-backoff:250ms}") val retryMinBackoff: Duration,
) {
  private val retryableStatusCodes = setOf(429, 502, 503, 504)

  @Bean
  fun retryingWebClientCustomizer(): WebClientCustomizer = WebClientCustomizer { builder ->
    builder.filter(retryingExchangeFilterFunction())
  }

  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  @ConditionalOnProperty(name = ["app.strategies.assessment"], havingValue = "true")
  fun sanApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    @Value("\${app.services.strengths-and-needs-api.base-url}") sanApiBaseUri: String,
  ): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "coordinator-api", url = sanApiBaseUri, timeout)

  @Bean
  @ConditionalOnProperty(name = ["app.strategies.plan"], havingValue = "true")
  fun sentencePlanApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    @Value("\${app.services.sentence-plan-api.base-url}") sentencePlanApiBaseUri: String,
  ): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "coordinator-api", url = sentencePlanApiBaseUri, timeout)

  @Bean
  @ConditionalOnProperty(name = ["app.strategies.aap-plan"], havingValue = "true")
  fun aapApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    @Value("\${app.services.aap-api.base-url}") aapApiBaseUri: String,
  ): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "coordinator-api", url = aapApiBaseUri, timeout)

  internal fun retryingExchangeFilterFunction(): ExchangeFilterFunction = ExchangeFilterFunction { request, next ->
    next.exchange(request)
      .flatMap(::toRetryableResponse)
      .retryWhen(createRetrySpec())
  }

  private fun toRetryableResponse(response: ClientResponse): Mono<ClientResponse> {
    if (!retryableStatusCodes.contains(response.statusCode().value())) {
      return Mono.just(response)
    }

    return response.createException().flatMap { exception ->
      Mono.error(exception)
    }
  }

  private fun createRetrySpec(): Retry = Retry.backoff(retryMaxRetries, retryMinBackoff)
    .jitter(0.5)
    .filter(::isRetryableException)
    .onRetryExhaustedThrow { _, signal -> signal.failure() }

  private fun isRetryableException(exception: Throwable): Boolean {
    if (exception is WebClientResponseException) {
      return retryableStatusCodes.contains(exception.statusCode.value())
    }

    if (exception is WebClientRequestException || exception is IOException) {
      return true
    }

    return exception.cause?.let(::isRetryableException) ?: false
  }
}

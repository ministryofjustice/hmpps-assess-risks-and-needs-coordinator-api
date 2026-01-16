package uk.gov.justice.digital.hmpps.arnscoordinatorapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${app.services.hmpps-auth.base-url}") val hmppsAuthBaseUri: String,
  @Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @Value("\${api.timeout:20s}") val timeout: Duration,
) {
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  @ConditionalOnProperty(name = ["app.strategies.assessment"], havingValue = "true")
  fun sanApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    @Value("\${app.services.strengths-and-needs-api.base-url}") sanApiBaseUri: String,
  ): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "san-api", url = sanApiBaseUri, timeout)

  @Bean
  @ConditionalOnProperty(name = ["app.strategies.plan"], havingValue = "true")
  fun sentencePlanApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    @Value("\${app.services.sentence-plan-api.base-url}") sentencePlanApiBaseUri: String,
  ): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "sentence-plan-api", url = sentencePlanApiBaseUri, timeout)

  @Bean
  @ConditionalOnProperty(name = ["app.strategies.aap-plan"], havingValue = "true")
  fun aapApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    @Value("\${app.services.aap-api.base-url}") aapApiBaseUri: String,
  ): WebClient = builder.authorisedWebClient(authorizedClientManager, registrationId = "aap-api", url = aapApiBaseUri, timeout)
}

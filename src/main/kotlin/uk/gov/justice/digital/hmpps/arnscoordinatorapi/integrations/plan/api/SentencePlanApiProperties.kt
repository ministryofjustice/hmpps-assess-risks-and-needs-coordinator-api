package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.services.sentence-plan-api")
data class SentencePlanApiProperties(
  var baseUrl: String = "",
  var endpoints: Endpoints = Endpoints(),
  var paths: Endpoints = Endpoints(),
) {

  data class Endpoints(
    var fetch: String = "",
    var create: String = "",
    var lock: String = "",
    var rollback: String = "",
  )

  @PostConstruct
  fun init() {
    paths = Endpoints(
      fetch = baseUrl + endpoints.fetch,
      create = baseUrl + endpoints.create,
      rollback = baseUrl + endpoints.rollback,
    )
  }
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.services.strengths-and-needs-api")
data class StrengthsAndNeedsApiProperties(
  var baseUrl: String = "",
  var endpoints: Endpoints = Endpoints(),
  var paths: Endpoints = Endpoints(),
) {

  data class Endpoints(
    var fetch: String = "",
    var create: String = "",
    var lock: String = "",
  )

  @PostConstruct
  fun init() {
    paths = Endpoints(
      fetch = baseUrl + endpoints.fetch,
      create = baseUrl + endpoints.create,
      lock = baseUrl + endpoints.lock,
    )
  }
}

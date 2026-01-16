package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.services.aap-api")
data class AAPApiProperties(
  var baseUrl: String = "",
  var endpoints: Endpoints = Endpoints(),
) {

  data class Endpoints(
    var command: String = "/command",
    var query: String = "/query",
  )
}

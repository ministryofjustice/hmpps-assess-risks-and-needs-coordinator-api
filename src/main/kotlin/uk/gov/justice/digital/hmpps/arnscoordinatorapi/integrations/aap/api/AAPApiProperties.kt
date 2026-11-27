package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.services.aap-api")
data class AAPApiProperties(
  var baseUrl: String = "",
  var endpoints: Endpoints = Endpoints(),
) {

  data class Endpoints(
    var command: String = "/command",
    var query: String = "/query",
    var delete: String = "/assessment/{uuid}",
    var resetPlan: String = "/plan/start-new-period-of-supervision",

  )
}

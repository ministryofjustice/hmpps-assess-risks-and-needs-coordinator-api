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
    var clone: String = "",
    var counterSign: String = "",
    var delete: String = "",
    var sign: String = "",
    var lock: String = "",
    var rollback: String = "",
    var softDelete: String = "",
    var undelete: String = "",
  )

  @PostConstruct
  fun init() {
    paths = Endpoints(
      fetch = baseUrl + endpoints.fetch,
      create = baseUrl + endpoints.create,
      clone = baseUrl + endpoints.clone,
      sign = baseUrl + endpoints.sign,
      lock = baseUrl + endpoints.lock,
      rollback = baseUrl + endpoints.rollback,
      counterSign = baseUrl + endpoints.counterSign,
      softDelete = baseUrl + endpoints.softDelete,
      undelete = baseUrl + endpoints.undelete,
    )
  }
}

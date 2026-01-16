package uk.gov.justice.digital.hmpps.arnscoordinatorapi.wiremock

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("wiremock")
@Configuration
@ConfigurationProperties(prefix = "wiremock")
data class WireMockProperties(
  var port: Int = 0,
  var paths: Paths = Paths(),
) {
  data class Paths(
    var strengthAndNeeds: String = "",
    var sentencePlan: String = "",
    var arnsAssessmentPlatform: String = "",
  )
}

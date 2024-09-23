package uk.gov.justice.digital.hmpps.arnscoordinatorapi.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("wiremock")
class WireMockInitialiser(
  private val wireMockProperties: WireMockProperties
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(WireMockInitialiser::class.java)
  }

  @Bean
  fun wireMockServer(): WireMockServer {
    val wireMockServer = WireMockServer(
      WireMockConfiguration()
        .port(wireMockProperties.port)
        .globalTemplating(true)
    )

    wireMockServer.start()
    log.info("WireMock server started on port ${wireMockServer.port()}")

    Runtime.getRuntime().addShutdownHook(Thread { wireMockServer.stop() })

    return wireMockServer
  }
}
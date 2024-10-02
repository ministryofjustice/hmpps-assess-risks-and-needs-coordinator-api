package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.SentencePlanApiMockExtension
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.SentencePlanApiMockExtension.Companion.sentencePlanApiMock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.StrengthsAndNeedsApiExtension
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.StrengthsAndNeedsApiExtension.Companion.sanServer
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ExtendWith(HmppsAuthApiExtension::class, StrengthsAndNeedsApiExtension::class, SentencePlanApiMockExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun stubGrantToken() {
    hmppsAuth.stubGrantToken()
  }

  protected fun stubAssessmentsCreate(status: Int = 201) {
    sanServer.stubAssessmentsCreate(status)
  }

  protected fun stubSentencePlanCreate(status: Int = 201) {
    sentencePlanApiMock.stubSentencePlanCreate(status)
  }

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
    sanServer.stubHealthPing(status)
  }
}

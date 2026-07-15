package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.TestBeanConfig
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.AAPApiMockExtension
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.AAPApiMockExtension.Companion.aapApiMock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.SentencePlanApiMockExtension
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.SentencePlanApiMockExtension.Companion.sentencePlanApiMock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.StrengthsAndNeedsApiExtension
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.StrengthsAndNeedsApiExtension.Companion.sanServer
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.RandomOasysPk
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentTypeConfig
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.util.UUID

@ExtendWith(HmppsAuthApiExtension::class, StrengthsAndNeedsApiExtension::class, SentencePlanApiMockExtension::class, AAPApiMockExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Import(TestBeanConfig::class)
abstract class IntegrationTestBase {

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  lateinit var randomOasysPk: RandomOasysPk

  @Autowired lateinit var assessmentTypeConfig: AssessmentTypeConfig

  fun getRandomOasysPk() = randomOasysPk.get()

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun stubGrantToken() {
    hmppsAuth.stubGrantToken()
  }

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
    sanServer.stubHealthPing(status)
    sentencePlanApiMock.stubHealthPing(status)
    aapApiMock.stubHealthPing(status)
  }

  protected fun stubAAPCreateAssessment(status: Int = 201, assessmentType: String, uuid: UUID) {
    aapApiMock.stubCreateAssessment(status, assessmentType, uuid)
  }

  protected fun stubAAPQueryAssessment(status: Int = 200, assessmentType: String, uuid: UUID) {
    aapApiMock.stubQueryAssessment(status, assessmentType, uuid)
  }

  protected fun stubAAPQueryPlanVersions(status: Int = 200, uuid: UUID) {
    aapApiMock.stubQueryPlanVersions(status, uuid)
  }

  protected fun stubAAPQuerySanVersions(status: Int = 200, uuid: UUID) {
    aapApiMock.stubQuerySanVersions(status, uuid)
  }

  protected fun stubAAPMarkMerged(status: Int = 200) {
    aapApiMock.stubMarkMerged(status)
  }

  protected fun stubAAPSoftDeleteAssessment(status: Int = 200) {
    aapApiMock.stubSoftDeleteAssessment(status)
  }
}

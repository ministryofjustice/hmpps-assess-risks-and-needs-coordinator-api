package uk.gov.justice.digital.hmpps.arnscoordinatorapi.wiremock.mocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.SentencePlanApiProperties
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.wiremock.WireMockProperties

@Profile("wiremock")
@Component
class SentencePlanApiMock(
  private val wireMockServer: WireMockServer,
  private val apiProperties: SentencePlanApiProperties,
  private val wireMockProperties: WireMockProperties,
) {

  @PostConstruct
  private fun createPlanStub() {
    wireMockServer.stubFor(
      post(urlEqualTo(wireMockProperties.paths.sentencePlan + apiProperties.endpoints.create))
        .withRequestBody(matchingJsonPath("$.planType"))
        .withRequestBody(matchingJsonPath("$.userDetails.id"))
        .withRequestBody(matchingJsonPath("$.userDetails.name"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "sentencePlanId": "{{randomValue type='UUID'}}",
                "sentencePlanVersion": 0
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  @PostConstruct
  private fun getPlanStub() {
    wireMockServer.stubFor(
      get(urlPathMatching(wireMockProperties.paths.sentencePlan + "/plan/([a-f0-9\\-]+)"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "sentencePlanId": "{{request.pathSegments.[2]}}",
                "sentencePlanVersion": 1,
                "planType": "INITIAL",
                "planComplete": "INCOMPLETE",
                "lastUpdatedTimestampSP": "1725494400"
              }
              """.trimIndent(),
            ),
        ),
    )
  }
}

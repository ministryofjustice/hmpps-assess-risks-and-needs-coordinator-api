package uk.gov.justice.digital.hmpps.arnscoordinatorapi.wiremock.mocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.StrengthsAndNeedsApiProperties
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.wiremock.WireMockProperties

@Profile("wiremock")
@Component
class StrengthsAndNeedsApiMock(
  private val wireMockServer: WireMockServer,
  private val apiProperties: StrengthsAndNeedsApiProperties,
  private val wireMockProperties: WireMockProperties,
) {

  @PostConstruct
  private fun createAssessmentStub() {
    wireMockServer.stubFor(
      post(urlEqualTo(wireMockProperties.paths.strengthAndNeeds + apiProperties.endpoints.create))
        .withRequestBody(WireMock.matchingJsonPath("$.userDetails.id"))
        .withRequestBody(WireMock.matchingJsonPath("$.userDetails.name"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "sanAssessmentId": "{{randomValue type='UUID'}}",
                "sanAssessmentVersion": 0
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  @PostConstruct
  private fun getAssessmentStub() {
    wireMockServer.stubFor(
      get(urlPathMatching(wireMockProperties.paths.strengthAndNeeds + "coordinator/assessment/([a-f0-9\\-]+)"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "sanAssessmentId": "{{request.pathSegments.[2]}}",
                "sanAssessmentVersion": 1,
                "sanAssessmentData": {
                  "someMockData": "mockSanData"
                },
                "lastUpdatedTimestampSAN": "1725494400"
              }
              """.trimIndent(),
            ),
        ),
    )
  }
}

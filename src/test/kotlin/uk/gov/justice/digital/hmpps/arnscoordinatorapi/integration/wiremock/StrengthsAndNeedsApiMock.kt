package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class StrengthsAndNeedsApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val sanServer = StrengthsAndNeedsApiMock()
  }

  override fun beforeAll(context: ExtensionContext) {
    sanServer.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    sanServer.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    sanServer.stop()
  }
}

class StrengthsAndNeedsApiMock : WireMockServer(8092) {
  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody("""{"status":"${if (status == 200) "UP" else "DOWN"}"}""")
          .withStatus(status),
      ),
    )
  }

  fun stubAssessmentsCreate(status: Int = 201) {
    stubFor(
      post("/coordinator/assessment").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "id": "90A71D16-FECD-4E1A-85B9-98178BF0F8D0",
                "version": 0
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubAssessmentsLock(status: Int = 200) {
    stubFor(
      post(WireMock.urlPathMatching("/coordinator/assessment/(.*)/lock")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "sanAssessmentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "sanAssessmentVersion": 0
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubAssessmentsGet(status: Int = 200) {
    stubFor(
      get(WireMock.urlPathMatching("/coordinator/assessment/.*")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "sanAssessmentId": "11db45b5-215d-4405-a887-a7efd5216fa2",
                "sanAssessmentVersion": 1,
                "sanAssessmentData": {
                  "metaData": {
                    "uuid": "11db45b5-215d-4405-a887-a7efd5216fa2",
                    "createdAt": "2024-10-03T15:22:31.452243",
                    "oasys_pks": [
                      "Msmp2OKxx7IWIEa",
                      "YWFj5GLPTasn9PR"
                    ],
                    "versionUuid": "d52fdb5d-4450-40af-806e-97d47b96fa57",
                    "versionCreatedAt": "2024-10-03T15:22:31.453096",
                    "versionTag": "UNSIGNED",
                    "formVersion": "1.0"
                  },
                  "assessment": {
                    "q2": {
                      "type": "TEXT",
                      "description": "",
                      "options": null,
                      "value": "val2",
                      "values": null,
                      "collection": null
                    }
                  },
                  "oasysEquivalent": {
                    "q2": "2"
                  }
                },
                "lastUpdatedTimestamp": "2024-10-02T15:22:31.452276"
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }
}

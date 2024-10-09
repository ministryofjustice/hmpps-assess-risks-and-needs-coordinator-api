package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class SentencePlanApiMock : WireMockServer(8091) {
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

  fun stubSentencePlanCreate(status: Int = 201) {
    stubFor(
      post("/coordinator/plan").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "planId": "4180ED3E-2412-4CA5-9B30-9ADD00941113",
                "planVersion": 0
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubSentencePlanGet(status: Int = 200) {
    stubFor(
      get(urlPathMatching("/coordinator/plan/.*")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "sentencePlanId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "sentencePlanVersion": 0,
                "planComplete": "COMPLETE",
                "planType": "INITIAL",
                "lastUpdatedTimestampSP": "2024-10-03T15:22:31.453096"
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubSentencePlanSign(status: Int = 200) {
    stubFor(
      post(urlPathMatching("/coordinator/plan/(.*)/sign")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "planId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "planVersion": 0
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubSentencePlanLock(status: Int = 200) {
    stubFor(
      post(urlPathMatching("/coordinator/plan/(.*)/lock")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "planId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "planVersion": 0
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubSentencePlanRollback(status: Int = 200) {
    stubFor(
      post(urlPathMatching("/coordinator/plan/(.*)/rollback")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "planId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "planVersion": 2
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubSentencePlanCounterSign(status: Int = 200) {
    stubFor(
      post(urlPathMatching("/coordinator/plan/(.*)/countersign")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "planId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                "planVersion": 1
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }
}

class SentencePlanApiMockExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val sentencePlanApiMock = SentencePlanApiMock()
  }

  override fun beforeAll(context: ExtensionContext): Unit = sentencePlanApiMock.start()
  override fun beforeEach(context: ExtensionContext): Unit = sentencePlanApiMock.resetAll()
  override fun afterAll(context: ExtensionContext): Unit = sentencePlanApiMock.stop()
}

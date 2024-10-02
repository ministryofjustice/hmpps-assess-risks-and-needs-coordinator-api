package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
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
      post("/assessment").willReturn(
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
}

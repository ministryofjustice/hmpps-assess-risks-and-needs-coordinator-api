package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
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

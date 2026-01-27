package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class AAPApiMock : WireMockServer(8093) {

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

  fun stubCreateAssessment(status: Int = 201) {
    stubFor(
      post("/command").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "commands": [
                  {
                    "request": null,
                    "result": {
                      "type": "CreateAssessmentCommandResult",
                      "assessmentUuid": "5fa85f64-5717-4562-b3fc-2c963f66afa6",
                      "message": "Assessment created successfully",
                      "success": true
                    }
                  }
                ]
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubQueryAssessment(status: Int = 200) {
    stubFor(
      post("/query").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "queries": [
                  {
                    "result": {
                      "assessmentUuid": "5fa85f64-5717-4562-b3fc-2c963f66afa6",
                      "aggregateUuid": "6fa85f64-5717-4562-b3fc-2c963f66afa6",
                      "assessmentType": "SENTENCE_PLAN",
                      "formVersion": "1.0",
                      "createdAt": "2026-01-09T12:00:00",
                      "updatedAt": "2026-01-09T12:30:00",
                      "answers": {},
                      "properties": {
                        "PLAN_TYPE": { "type": "Single", "value": "INITIAL" }
                      },
                      "collections": [],
                      "collaborators": [],
                      "identifiers": {}
                    }
                  }
                ]
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }
}

class AAPApiMockExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val aapApiMock = AAPApiMock()
  }

  override fun beforeAll(context: ExtensionContext): Unit = aapApiMock.start()

  override fun beforeEach(context: ExtensionContext): Unit = aapApiMock.resetAll()

  override fun afterAll(context: ExtensionContext): Unit = aapApiMock.stop()
}

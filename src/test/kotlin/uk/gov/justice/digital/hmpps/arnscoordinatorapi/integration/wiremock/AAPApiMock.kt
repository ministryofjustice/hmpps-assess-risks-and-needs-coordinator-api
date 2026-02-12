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
                    "request": {
                      "type": "CreateAssessmentCommand",
                      "assessmentType": "SENTENCE_PLAN",
                      "formVersion": "",
                      "user": { "id": 1, "name": "Test Name" }
                    },
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
                    "request": {
                      "type": "AssessmentVersionQuery",
                      "user": { "id": "COORDINATOR_API", "name": "Coordinator API User" },
                      "assessmentIdentifier": { "type": "UUID", "uuid": "5fa85f64-5717-4562-b3fc-2c963f66afa6" }
                    },
                    "result": {
                      "type": "AssessmentVersionQueryResult",
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

  fun stubQueryAssessmentVersions(status: Int = 200) {
    stubFor(
      post("/query").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "queries": [
                  {
                    "request": {
                      "type": "DailyVersionsQuery",
                      "user": { "id": "COORDINATOR_API", "name": "Coordinator API User" },
                      "assessmentIdentifier": { "type": "UUID", "uuid": "5fa85f64-5717-4562-b3fc-2c963f66afa6" }
                    },
                    "result": {
                      "type": "DailyVersionsQueryResult",
                      "versions": [
                        {
                          "createdAt": "2025-04-23T14:40:53.105",
                          "updatedAt": "2025-04-23T14:40:53.105",
                          "lastTimelineItemUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                        },
                        {
                          "createdAt": "2025-05-24T14:40:53.105",
                          "updatedAt": "2025-05-24T14:40:53.105",
                          "lastTimelineItemUuid": "645951e9-15ed-43a1-ac8b-19e97ae0ddf1"
                        }
                      ]
                    }
                  },
                  {
                    "request": {
                      "type": "TimelineQuery",
                      "user": { "id": "COORDINATOR_API", "name": "Coordinator API User" },
                      "assessmentIdentifier": { "type": "UUID", "uuid": "5fa85f64-5717-4562-b3fc-2c963f66afa6" }
                    },
                    "result": {
                      "type": "TimelineQueryResult",
                      "pageInfo": {
                        "pageNumber": 0,
                        "totalPages": 1
                      },
                      "timeline": [
                        {
                          "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                          "timestamp": "2025-04-23T14:40:53.105Z",
                          "user": { "id": 1, "name": "Test User" },
                          "assessment": "5fa85f64-5717-4562-b3fc-2c963f66afa6",
                          "event": "ASSESSMENT_ANSWERS_UPDATED",
                          "customType": "PLAN_AGREEMENT_STATUS_CHANGED",
                          "customData": { "status": "AGREED" }
                        }
                      ]
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

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

class StrengthsAndNeedsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
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
                "metaData": {
                  "uuid": "90a71d16-fecd-4e1a-85b9-98178bf0f8d0",
                  "createdAt": "2024-10-03T15:22:31.452243",
                  "versionUuid": "d52fdb5d-4450-40af-806e-97d47b96fa57",
                  "versionCreatedAt": "2024-10-03T15:22:31.453096",
                  "versionUpdatedAt": "2024-10-04T15:22:31.453096",
                  "versionNumber": 1,
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
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubAssessmentsClone(status: Int = 200) {
    stubFor(
      post(WireMock.urlPathMatching("/assessment/(.*)/clone")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "metaData": {
                  "uuid": "90a71d16-fecd-4e1a-85b9-98178bf0f8d0",
                  "createdAt": "2024-10-03T15:22:31.452243",
                  "versionUuid": "d52fdb5d-4450-40af-806e-97d47b96fa57",
                  "versionCreatedAt": "2024-10-03T15:22:31.453096",
                  "versionUpdatedAt": "2024-10-04T15:22:31.453096",
                  "versionNumber": 1,
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
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubAssessmentsSign(status: Int = 200) {
    stubFor(
      post(WireMock.urlPathMatching("/assessment/(.*)/sign")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "metaData": {
                  "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "createdAt": "2024-10-03T15:22:31.452243",
                  "versionUuid": "d52fdb5d-4450-40af-806e-97d47b96fa57",
                  "versionCreatedAt": "2024-10-03T15:22:31.453096",
                  "versionUpdatedAt": "2024-10-04T15:22:31.453096",
                  "versionNumber": 0,
                  "versionTag": "AWAITING_COUNTERSIGN",
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
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubAssessmentsLock(status: Int = 200) {
    stubFor(
      post(WireMock.urlPathMatching("/assessment/(.*)/lock")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "metaData": {
                  "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "createdAt": "2024-10-03T15:22:31.452243",
                  "versionUuid": "d52fdb5d-4450-40af-806e-97d47b96fa57",
                  "versionCreatedAt": "2024-10-03T15:22:31.453096",
                  "versionUpdatedAt": "2024-10-04T15:22:31.453096",
                  "versionNumber": 0,
                  "versionTag": "LOCKED_INCOMPLETE",
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
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubAssessmentsUndelete(status: Int = 200) {
    stubFor(
      post(WireMock.urlPathMatching("/assessment/(.*)/undelete")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "metaData": {
                  "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "createdAt": "2024-10-03T15:22:31.452243",
                  "versionUuid": "d52fdb5d-4450-40af-806e-97d47b96fa57",
                  "versionCreatedAt": "2024-10-03T15:22:31.453096",
                  "versionUpdatedAt": "2024-10-04T15:22:31.453096",
                  "versionNumber": 0,
                  "versionTag": "LOCKED_INCOMPLETE",
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
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubAssessmentsRollback(status: Int = 200) {
    stubFor(
      post(WireMock.urlPathMatching("/assessment/(.*)/rollback")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "metaData": {
                  "uuid": "4da85f64-5717-4562-b3fc-2c963f66afb8",
                  "createdAt": "2024-10-03T15:22:31.452243",
                  "versionUuid": "b42fdb5d-4450-40af-806e-97d47b96fa45",
                  "versionCreatedAt": "2024-10-03T15:22:31.453096",
                  "versionUpdatedAt": "2024-10-04T15:22:31.453096",
                  "versionNumber": 1,
                  "versionTag": "ROLLED_BACK",
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
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubAssessmentsCounterSign(status: Int = 200) {
    stubFor(
      post(WireMock.urlPathMatching("/assessment/(.*)/counter-sign")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "metaData": {
                  "uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "createdAt": "2024-10-03T15:22:31.452243",
                  "versionUuid": "d52fdb5d-4450-40af-806e-97d47b96fa57",
                  "versionCreatedAt": "2024-10-03T15:22:31.453096",
                  "versionUpdatedAt": "2024-10-04T15:22:31.453096",
                  "versionNumber": 1,
                  "versionTag": "COUNTERSIGNED",
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
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubAssessmentsGet(status: Int = 200) {
    stubFor(
      get(WireMock.urlPathMatching("/assessment/.*")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
              {
                "metaData": {
                  "uuid": "11db45b5-215d-4405-a887-a7efd5216fa2",
                  "createdAt": "2024-10-03T15:22:31.452243",
                  "versionUuid": "d52fdb5d-4450-40af-806e-97d47b96fa57",
                  "versionCreatedAt": "2024-10-03T15:22:31.453096",
                  "versionUpdatedAt": "2024-10-04T15:22:31.453096",
                  "versionNumber": 1,
                  "versionTag": "UNSIGNED",
                  "formVersion": "1.0"
                },
                "assessment": {
                  "q2": {
                    "type": "TEXT",
                    "description": "",
                    "options": null,
                    "value": "Question answer &amp;, &#x27;, &lt;, &gt;, &#x2F;, &#x5C;, &#96;, &quot;",
                    "values": null,
                    "collection": null
                  }
                },
                "oasysEquivalent": {
                  "q2": "Question answer &amp;, &#x27;, &lt;, &gt;, &#x2F;, &#x5C;, &#96;, &quot;"
                }
              }
            """.trimIndent(),
          )
          .withStatus(status),
      ),
    )
  }

  fun stubAssessmentsSoftDelete(status: Int = 200, emptyBody: Boolean = false) {
    stubFor(
      post(WireMock.urlPathMatching("/assessment/(.*)/soft-delete")).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(status)
          .run {
            if (emptyBody) {
              this
            } else {
              withBody(
                """
                {
                  "metaData": {
                    "uuid": "61369578-18f5-488c-bc99-7cc6249f39a2",
                    "createdAt": "2024-10-03T15:22:31.452243",
                    "versionUuid": "d52fdb5d-4450-40af-806e-97d47b96fa57",
                    "versionCreatedAt": "2024-10-03T15:22:31.453096",
                    "versionUpdatedAt": "2024-10-04T15:22:31.453096",
                    "versionNumber": 2,
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
                }
                """.trimIndent(),
              )
            }
          },
      ),
    )
  }
}

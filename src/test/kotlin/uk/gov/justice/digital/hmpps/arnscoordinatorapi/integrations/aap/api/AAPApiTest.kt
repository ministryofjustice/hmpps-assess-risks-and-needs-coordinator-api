package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.ConflictException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.CoordinatorException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.UpdateFlagsCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.DailyVersionsQuery
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.Query
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command.CommandSuccessResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.DailyVersionsQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.QueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.SingleValue
import java.time.LocalDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AAPApiTest {
  private val server = WireMockServer(options().dynamicPort())
  private lateinit var aapApi: AAPApi

  private val user = AAPUser(id = "user-id", name = "User Name")
  private val assessmentUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6")

  @BeforeAll
  fun startServer() {
    server.start()
    aapApi = AAPApi(WebClient.builder().baseUrl(server.baseUrl()).build())
  }

  @AfterAll
  fun stopServer() = server.stop()

  @BeforeEach
  fun reset() = server.resetAll()

  private fun stub(path: String, status: Int, body: String) {
    server.stubFor(post(path).willReturn(aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body)))
  }

  @Nested
  inner class DispatchCommands {
    @Test
    fun `posts the commands with their type names and returns the results in order`() {
      stub(
        "/command",
        200,
        """
        {
          "commands": [
            {
              "request": { "type": "CreateAssessmentCommand", "assessmentType": "SENTENCE_PLAN", "formVersion": "v1.0", "user": { "id": "user-id", "name": "User Name" } },
              "result": { "type": "CreateAssessmentCommandResult", "assessmentUuid": "$assessmentUuid", "message": "Created", "success": true }
            },
            {
              "request": { "type": "UpdateFlagsCommand", "assessmentUuid": "$assessmentUuid", "flags": [], "user": { "id": "user-id", "name": "User Name" } },
              "result": { "type": "CommandSuccessCommandResult", "message": "Done", "success": true }
            }
          ]
        }
        """.trimIndent(),
      )

      val response = aapApi.dispatchCommands(
        listOf(
          CreateAssessmentCommand(assessmentType = "SENTENCE_PLAN", formVersion = "v1.0", user = user),
          UpdateFlagsCommand(assessmentUuid = assessmentUuid, user = user, flags = emptyList()),
        ),
      )

      server.verify(
        postRequestedFor(urlEqualTo("/command")).withRequestBody(
          equalToJson(
            """
            {
              "commands": [
                { "type": "CreateAssessmentCommand", "assessmentType": "SENTENCE_PLAN", "formVersion": "v1.0", "user": { "id": "user-id", "name": "User Name" } },
                { "type": "UpdateFlagsCommand", "assessmentUuid": "$assessmentUuid", "flags": [], "user": { "id": "user-id", "name": "User Name" } }
              ]
            }
            """.trimIndent(),
            true,
            true,
          ),
        ),
      )
      assertThat(response.commands.map { it.result }).containsExactly(
        CreateAssessmentCommandResult(assessmentUuid = assessmentUuid, message = "Created"),
        CommandSuccessResult(message = "Done"),
      )
      assertThat(response.extractNthInstance<CreateAssessmentCommandResult>(0).assessmentUuid).isEqualTo(assessmentUuid)
    }

    @Test
    fun `throws a conflict when AAP responds with 409`() {
      stub("/command", 409, """{ "status": 409, "userMessage": "Nope", "developerMessage": "Already undeleted" }""")

      val exception = assertThrows<ConflictException> {
        aapApi.dispatchCommands(listOf(UpdateFlagsCommand(assessmentUuid = assessmentUuid, user = user, flags = emptyList())))
      }

      assertThat(exception.statusCode).isEqualTo(HttpStatus.CONFLICT)
      assertThat(exception.message).isEqualTo("Call to Assessment Platform API failed with status 409: Already undeleted")
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 404, 500])
    fun `throws a coordinator error for any other error status`(status: Int) {
      stub("/command", status, """{ "status": $status, "userMessage": "Broken", "developerMessage": "Something broke" }""")

      val exception = assertThrows<CoordinatorException> {
        aapApi.dispatchCommands(listOf(UpdateFlagsCommand(assessmentUuid = assessmentUuid, user = user, flags = emptyList())))
      }

      assertThat(exception).isNotInstanceOf(ConflictException::class.java)
      assertThat(exception.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
      assertThat(exception.message).isEqualTo("Call to Assessment Platform API failed with status $status: Something broke")
    }

    @Test
    fun `still throws when the error response has no readable body`() {
      server.stubFor(post("/command").willReturn(aResponse().withStatus(500).withBody("<html>Bad gateway</html>")))

      val exception = assertThrows<CoordinatorException> {
        aapApi.dispatchCommands(listOf(UpdateFlagsCommand(assessmentUuid = assessmentUuid, user = user, flags = emptyList())))
      }

      assertThat(exception.message).isEqualTo("Call to Assessment Platform API failed with status 500: ")
    }
  }

  @Nested
  inner class DispatchQueries {
    private val timestamp = LocalDateTime.parse("2026-01-09T12:00:00")

    private val responseBody = """
      {
        "queries": [
          {
            "request": { "type": "AssessmentVersionQuery", "user": { "id": "user-id", "name": "User Name" }, "assessmentIdentifier": { "type": "UUID", "uuid": "$assessmentUuid" } },
            "result": {
              "type": "AssessmentVersionQueryResult",
              "assessmentUuid": "$assessmentUuid",
              "aggregateUuid": "6fa85f64-5717-4562-b3fc-2c963f66afa6",
              "assessmentType": "SENTENCE_PLAN",
              "formVersion": "v1.0",
              "createdAt": "2026-01-09T12:00:00",
              "updatedAt": "2026-01-09T12:30:00",
              "answers": {},
              "properties": { "PLAN_TYPE": { "type": "Single", "value": "INITIAL" } },
              "collections": [],
              "collaborators": [],
              "identifiers": {}
            }
          },
          {
            "request": { "type": "DailyVersionsQuery", "user": { "id": "user-id", "name": "User Name" }, "assessmentIdentifier": { "type": "UUID", "uuid": "$assessmentUuid" } },
            "result": {
              "type": "DailyVersionsQueryResult",
              "versions": [ { "createdAt": "2025-04-23T14:40:53.105", "updatedAt": "2025-04-23T14:40:53.105", "lastTimelineItemUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6" } ]
            }
          }
        ]
      }
    """.trimIndent()

    private val queries = listOf<Query>(
      AssessmentVersionQuery(user = user, assessmentIdentifier = AssessmentIdentifier(assessmentUuid), timestamp = timestamp),
      DailyVersionsQuery(user = user, assessmentIdentifier = AssessmentIdentifier(assessmentUuid)),
    )

    @Test
    fun `posts the queries and returns the typed results`() {
      stub("/query", 200, responseBody)

      val response = aapApi.dispatchQueries(queries)

      server.verify(
        postRequestedFor(urlEqualTo("/query")).withRequestBody(
          equalToJson(
            """
            {
              "queries": [
                { "type": "AssessmentVersionQuery", "assessmentIdentifier": { "type": "UUID", "uuid": "$assessmentUuid" }, "timestamp": "2026-01-09T12:00:00" },
                { "type": "DailyVersionsQuery", "assessmentIdentifier": { "type": "UUID", "uuid": "$assessmentUuid" } }
              ]
            }
            """.trimIndent(),
            true,
            true,
          ),
        ),
      )
      val assessment = response.extractNthInstance<AssessmentVersionQueryResult>(0)
      assertThat(assessment.assessmentUuid).isEqualTo(assessmentUuid)
      assertThat(assessment.properties["PLAN_TYPE"]).isEqualTo(SingleValue("INITIAL"))
      assertThat(response.extractSingle<DailyVersionsQueryResult>().versions).hasSize(1)
    }

    @Test
    fun `passes each result to the callback of the query it answers`() {
      stub("/query", 200, responseBody)
      val received = mutableListOf<Pair<Int, QueryResult>>()

      aapApi.dispatchQueries(queries.mapIndexed { index, query -> query to { result: QueryResult -> received.add(index to result) } })

      assertThat(received.map { it.first }).containsExactly(0, 1)
      assertThat(received[0].second).isInstanceOf(AssessmentVersionQueryResult::class.java)
      assertThat(received[1].second).isInstanceOf(DailyVersionsQueryResult::class.java)
    }

    @Test
    fun `throws a coordinator error when AAP fails`() {
      stub("/query", 500, """{ "status": 500, "developerMessage": "Query failed" }""")

      val exception = assertThrows<CoordinatorException> { aapApi.dispatchQueries(queries) }

      assertThat(exception.message).isEqualTo("Call to Assessment Platform API failed with status 500: Query failed")
    }
  }
}

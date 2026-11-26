package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.UUID

/**
 * Fake Assessment Platform API.
 *
 * The coordinator sends batches of commands and queries, and reads each result back by its position in the batch,
 * so every request is answered item by item from the state registered by the test:
 * - CreateAssessmentCommand gets the UUID registered for its assessment type (or a random one)
 * - other commands succeed
 * - AssessmentVersionQuery, DailyVersionsQuery and TimelineQuery return what was registered for the assessment
 *
 * A failure status can be registered for a command type, a create of a given assessment type, or an assessment's queries.
 */
class AAPApiMock private constructor(private val state: State) : WireMockServer(options().port(8093).extensions(BatchResponder(state))) {
  constructor() : this(State())

  fun reset() {
    resetAll()
    state.clear()
    listOf("/command", "/query").forEach { path ->
      stubFor(post(path).willReturn(aResponse().withTransformers(BatchResponder.NAME)))
    }
  }

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

  fun stubCreateAssessment(status: Int = 201, assessmentType: String, uuid: UUID) {
    state.createdUuids[assessmentType] = uuid
    if (status >= 400) state.commandFailures["CreateAssessmentCommand:$assessmentType"] = status
  }

  fun stubUpdateFlags(status: Int = 200) = stubCommand("UpdateFlagsCommand", status)

  fun stubMarkMerged(status: Int = 200) = stubCommand("UpdateAssessmentPropertiesCommand", status)

  fun stubSoftDeleteAssessment(status: Int = 200) = stubCommand("SoftDeleteCommand", status)

  fun stubUndeleteAssessment(status: Int = 200) = stubCommand("UndeleteCommand", status)

  fun stubCommand(type: String, status: Int = 200) {
    if (status >= 400) state.commandFailures[type] = status else state.commandFailures.remove(type)
  }

  fun stubQueryAssessment(
    status: Int = 200,
    assessmentType: String,
    uuid: UUID,
    answers: Map<String, Any> = emptyMap(),
    properties: Map<String, Any> = mapOf("PLAN_TYPE" to single("INITIAL")),
    collections: List<Any> = emptyList(),
    updatedAt: String = "2026-01-09T12:30:00",
  ) {
    state.assessments[uuid] = mapOf(
      "type" to "AssessmentVersionQueryResult",
      "assessmentUuid" to uuid,
      "aggregateUuid" to UUID.randomUUID(),
      "assessmentType" to assessmentType,
      "formVersion" to "v1.0",
      "createdAt" to "2026-01-09T12:00:00",
      "updatedAt" to updatedAt,
      "answers" to answers,
      "properties" to properties,
      "collections" to collections,
      "collaborators" to emptyList<Any>(),
      "identifiers" to emptyMap<String, Any>(),
    )
    if (status >= 400) state.queryFailures[uuid] = status
  }

  fun stubQueryPlanVersions(status: Int = 200, uuid: UUID) {
    state.dailyVersions[uuid] = listOf(
      dailyVersion("2025-04-23T14:40:53.105", "3fa85f64-5717-4562-b3fc-2c963f66afa6"),
      dailyVersion("2025-05-24T14:40:53.105", "645951e9-15ed-43a1-ac8b-19e97ae0ddf1"),
    )
    state.timelines[uuid] = listOf(
      mapOf(
        "uuid" to "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "timestamp" to "2025-04-23T14:40:53.105",
        "user" to mapOf("id" to "1", "name" to "Test User"),
        "assessment" to uuid,
        "event" to "ASSESSMENT_ANSWERS_UPDATED",
        "customType" to "PLAN_AGREEMENT_STATUS_CHANGED",
        "customData" to mapOf("status" to "AGREED"),
      ),
    )
    if (status >= 400) state.queryFailures[uuid] = status
  }

  fun stubQuerySanVersions(status: Int = 200, uuid: UUID) {
    state.dailyVersions[uuid] = listOf(dailyVersion("2025-05-23T13:22:54.105", "61369578-18f5-488c-bc99-7cc6249f39a2"))
    if (status >= 400) state.queryFailures[uuid] = status
  }

  /** Every command sent to AAP of the given type, across all requests, in the order they were sent. */
  fun commandsSent(type: String): List<JsonNode> = itemsSent("/command", "commands").filter { it.path("type").asText() == type }

  /** Every query sent to AAP, across all requests, in the order they were sent. */
  fun queriesSent(): List<JsonNode> = itemsSent("/query", "queries")

  // Asserted against the captured bodies rather than equalToJson: WireMock's ignoreExtraElements
  // also ignores extra array items, so an expected "flags": [] would match an actual ["SAN_BETA"].
  fun verifyUpdateFlags(assessmentUuid: UUID, flags: List<String>) {
    val commands = commandsSent("UpdateFlagsCommand").filter { it.path("assessmentUuid").asText() == assessmentUuid.toString() }

    assertThat(commands)
      .describedAs("UpdateFlagsCommand requests for assessment $assessmentUuid")
      .hasSize(1)
    assertThat(commands.single().path("flags").map { it.asText() }).isEqualTo(flags)
    assertThat(commands.single().path("timeline").path("type").asText()).isEqualTo("FLAGS_UPDATED")
  }

  fun verifyUndeleteAssessmentNotCalled() {
    assertThat(commandsSent("UndeleteCommand")).isEmpty()
  }

  private fun itemsSent(path: String, field: String): List<JsonNode> = findAll(postRequestedFor(urlEqualTo(path)))
    .sortedBy { it.loggedDate }
    .flatMap { mapper.readTree(it.bodyAsString).path(field).toList() }

  private fun dailyVersion(at: String, lastTimelineItemUuid: String) = mapOf(
    "createdAt" to at,
    "updatedAt" to at,
    "lastTimelineItemUuid" to lastTimelineItemUuid,
  )

  private class State {
    val createdUuids = mutableMapOf<String, UUID>()
    val commandFailures = mutableMapOf<String, Int>()
    val assessments = mutableMapOf<UUID, Map<String, Any>>()
    val dailyVersions = mutableMapOf<UUID, List<Map<String, Any>>>()
    val timelines = mutableMapOf<UUID, List<Map<String, Any>>>()
    val queryFailures = mutableMapOf<UUID, Int>()

    fun clear() {
      createdUuids.clear()
      commandFailures.clear()
      assessments.clear()
      dailyVersions.clear()
      timelines.clear()
      queryFailures.clear()
    }
  }

  private class BatchResponder(private val state: State) : ResponseDefinitionTransformerV2 {
    override fun getName() = NAME

    override fun applyGlobally() = false

    override fun transform(serveEvent: ServeEvent): ResponseDefinition {
      val body = mapper.readTree(serveEvent.request.bodyAsString)

      return when (serveEvent.request.url) {
        "/command" -> respond("commands", body.path("commands").toList()) { command -> commandResult(command) }
        "/query" -> respond("queries", body.path("queries").toList()) { query -> queryResult(query) }
        else -> error("Unexpected AAP request to ${serveEvent.request.url}")
      }
    }

    private fun respond(field: String, items: List<JsonNode>, resultFor: (JsonNode) -> Result): ResponseDefinition {
      val results = items.map(resultFor)

      results.filterIsInstance<Result.Failure>().firstOrNull()?.let { failure ->
        return json(
          failure.status,
          mapOf("status" to failure.status, "userMessage" to failure.message, "developerMessage" to failure.message),
        )
      }

      return json(
        200,
        mapOf(field to items.zip(results).map { (request, result) -> mapOf("request" to request, "result" to (result as Result.Success).body) }),
      )
    }

    private fun commandResult(command: JsonNode): Result {
      val type = command.path("type").asText()
      val assessmentType = command.path("assessmentType").asText()

      (state.commandFailures["$type:$assessmentType"] ?: state.commandFailures[type])?.let { status ->
        return Result.Failure(status, "$type failed")
      }

      return Result.Success(
        when (type) {
          "CreateAssessmentCommand" -> mapOf(
            "type" to "CreateAssessmentCommandResult",
            "assessmentUuid" to (state.createdUuids[assessmentType] ?: UUID.randomUUID()),
            "message" to "Assessment created successfully",
            "success" to true,
          )
          else -> mapOf("type" to "CommandSuccessCommandResult", "message" to "Done", "success" to true)
        },
      )
    }

    private fun queryResult(query: JsonNode): Result {
      val uuid = UUID.fromString(query.path("assessmentIdentifier").path("uuid").asText())

      state.queryFailures[uuid]?.let { return Result.Failure(it, "Query for $uuid failed") }

      return when (query.path("type").asText()) {
        "AssessmentVersionQuery" -> state.assessments[uuid]
          ?.let { Result.Success(it) }
          ?: Result.Failure(404, "No assessment found for $uuid")
        "DailyVersionsQuery" -> Result.Success(
          mapOf("type" to "DailyVersionsQueryResult", "versions" to state.dailyVersions[uuid].orEmpty()),
        )
        "TimelineQuery" -> Result.Success(
          mapOf(
            "type" to "TimelineQueryResult",
            "timeline" to state.timelines[uuid].orEmpty(),
            "pageInfo" to mapOf("pageNumber" to 0, "totalPages" to 1),
          ),
        )
        else -> error("Unexpected AAP query ${query.path("type").asText()}")
      }
    }

    private fun json(status: Int, body: Any): ResponseDefinition = aResponse()
      .withStatus(status)
      .withHeader("Content-Type", "application/json")
      .withBody(mapper.writeValueAsString(body))
      .build()

    sealed interface Result {
      data class Success(val body: Any) : Result
      data class Failure(val status: Int, val message: String) : Result
    }

    companion object {
      const val NAME = "aap-batch-responder"
    }
  }

  companion object {
    private val mapper = ObjectMapper()

    fun single(value: String) = mapOf("type" to "Single", "value" to value)

    fun multi(vararg values: String) = mapOf("type" to "Multi", "values" to values.toList())
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

  override fun beforeAll(context: ExtensionContext) {
    if (!aapApiMock.isRunning) aapApiMock.start()
  }

  override fun beforeEach(context: ExtensionContext): Unit = aapApiMock.reset()

  override fun afterAll(context: ExtensionContext): Unit = aapApiMock.stop()
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.CommandResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.CommandsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.IdentifierType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.QueriesResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.QueryResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.time.LocalDateTime
import java.util.UUID

class AAPApiTest {
  private val webClient: WebClient = mock()
  private val requestBodyUriSpec: WebClient.RequestBodyUriSpec = mock()
  private val requestBodySpec: WebClient.RequestBodySpec = mock()
  private val requestHeadersSpec: WebClient.RequestHeadersSpec<*> = mock()
  private val responseSpec: WebClient.ResponseSpec = mock()

  private lateinit var aapApi: AAPApi

  @BeforeEach
  fun setup() {
    val apiProperties = AAPApiProperties(
      baseUrl = "http://test-aap-api",
      endpoints = AAPApiProperties.Endpoints(command = "/command", query = "/query"),
    )
    aapApi = AAPApi(webClient, apiProperties)
  }

  @Nested
  inner class CreateAssessment {

    @Test
    fun `should return success when AAP API returns a valid response`() {
      val assessmentUuid = UUID.randomUUID()
      val createPlanData = CreatePlanData(PlanType.INITIAL, UserDetails("user-id", "User Name"))

      val commandResult = CreateAssessmentCommandResult(assessmentUuid = assessmentUuid)
      val response = CommandsResponse(
        commands = listOf(
          CommandResponse(request = null, result = commandResult),
        ),
      )

      `when`(webClient.post()).thenReturn(requestBodyUriSpec)
      `when`(requestBodyUriSpec.uri("/command")).thenReturn(requestBodySpec)
      `when`(requestBodySpec.body(any())).thenReturn(requestHeadersSpec)
      `when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      `when`(responseSpec.bodyToMono(CommandsResponse::class.java)).thenReturn(Mono.just(response))

      val result = aapApi.createAssessment(createPlanData)

      assertTrue(result is AAPApi.ApiOperationResult.Success)
      val successResult = result as AAPApi.ApiOperationResult.Success
      assertEquals(assessmentUuid, successResult.data.id)
      assertEquals(0, successResult.data.version)
      assertEquals(EntityType.AAP_PLAN, successResult.data.entityType)
    }

    @Test
    fun `should return failure when AAP API returns HTTP error`() {
      val createPlanData = CreatePlanData(PlanType.INITIAL, UserDetails("user-id", "User Name"))

      `when`(webClient.post()).thenReturn(requestBodyUriSpec)
      `when`(requestBodyUriSpec.uri("/command")).thenReturn(requestBodySpec)
      `when`(requestBodySpec.body(any())).thenReturn(requestHeadersSpec)
      `when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      `when`(responseSpec.bodyToMono(CommandsResponse::class.java))
        .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.BAD_REQUEST.value(), "Bad Request", null, "Error body".toByteArray(), null)))

      val result = aapApi.createAssessment(createPlanData)

      assertTrue(result is AAPApi.ApiOperationResult.Failure)
      val failureResult = result as AAPApi.ApiOperationResult.Failure
      assertTrue(failureResult.errorMessage.contains("HTTP error during create AAP assessment"))
    }

    @Test
    fun `should return failure when unexpected exception occurs`() {
      val createPlanData = CreatePlanData(PlanType.INITIAL, UserDetails("user-id", "User Name"))

      `when`(webClient.post()).thenReturn(requestBodyUriSpec)
      `when`(requestBodyUriSpec.uri("/command")).thenReturn(requestBodySpec)
      `when`(requestBodySpec.body(any())).thenReturn(requestHeadersSpec)
      `when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      `when`(responseSpec.bodyToMono(CommandsResponse::class.java))
        .thenReturn(Mono.error(RuntimeException("Unexpected error")))

      val result = aapApi.createAssessment(createPlanData)

      assertTrue(result is AAPApi.ApiOperationResult.Failure)
      val failureResult = result as AAPApi.ApiOperationResult.Failure
      assertTrue(failureResult.errorMessage.contains("Unexpected error during createAssessment"))
    }
  }

  @Nested
  inner class FetchAssessment {

    @Test
    fun `should return success when AAP API returns a valid response`() {
      // Arrange
      val assessmentUuid = UUID.randomUUID()
      val aggregateUuid = UUID.randomUUID()
      val now = LocalDateTime.now()

      val queryResult = AssessmentVersionQueryResult(
        assessmentUuid = assessmentUuid,
        aggregateUuid = aggregateUuid,
        assessmentType = "SENTENCE_PLAN",
        formVersion = "1.0",
        createdAt = now,
        updatedAt = now,
        answers = emptyMap(),
        properties = emptyMap(),
        collections = emptyList(),
        collaborators = emptySet(),
        identifiers = mapOf(IdentifierType.CRN to "X123456"),
      )
      val response = QueriesResponse(
        queries = listOf(QueryResponse(result = queryResult)),
      )

      `when`(webClient.post()).thenReturn(requestBodyUriSpec)
      `when`(requestBodyUriSpec.uri("/query")).thenReturn(requestBodySpec)
      `when`(requestBodySpec.body(any())).thenReturn(requestHeadersSpec)
      `when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      `when`(responseSpec.bodyToMono(QueriesResponse::class.java)).thenReturn(Mono.just(response))

      // Act
      val result = aapApi.fetchAssessment(assessmentUuid, now)

      // Assert
      assertTrue(result is AAPApi.ApiOperationResult.Success)
      val successResult = result as AAPApi.ApiOperationResult.Success
      assertEquals(assessmentUuid, successResult.data.assessmentUuid)
      assertEquals(aggregateUuid, successResult.data.aggregateUuid)
      assertEquals("SENTENCE_PLAN", successResult.data.assessmentType)
    }

    @Test
    fun `should return failure when AAP API returns HTTP error`() {
      // Arrange
      val entityUuid = UUID.randomUUID()
      val timestamp = LocalDateTime.now()

      `when`(webClient.post()).thenReturn(requestBodyUriSpec)
      `when`(requestBodyUriSpec.uri("/query")).thenReturn(requestBodySpec)
      `when`(requestBodySpec.body(any())).thenReturn(requestHeadersSpec)
      `when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      `when`(responseSpec.bodyToMono(QueriesResponse::class.java))
        .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.NOT_FOUND.value(), "Not Found", null, "Error body".toByteArray(), null)))

      // Act
      val result = aapApi.fetchAssessment(entityUuid, timestamp)

      // Assert
      assertTrue(result is AAPApi.ApiOperationResult.Failure)
      val failureResult = result as AAPApi.ApiOperationResult.Failure
      assertTrue(failureResult.errorMessage.contains("HTTP error during fetch AAP assessment"))
    }

    @Test
    fun `should return failure when AAP API returns empty queries list`() {
      // Arrange
      val entityUuid = UUID.randomUUID()
      val timestamp = LocalDateTime.now()
      val response = QueriesResponse(queries = emptyList())

      `when`(webClient.post()).thenReturn(requestBodyUriSpec)
      `when`(requestBodyUriSpec.uri("/query")).thenReturn(requestBodySpec)
      `when`(requestBodySpec.body(any())).thenReturn(requestHeadersSpec)
      `when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      `when`(responseSpec.bodyToMono(QueriesResponse::class.java)).thenReturn(Mono.just(response))

      // Act
      val result = aapApi.fetchAssessment(entityUuid, timestamp)

      // Assert
      assertTrue(result is AAPApi.ApiOperationResult.Failure)
      val failureResult = result as AAPApi.ApiOperationResult.Failure
      assertTrue(failureResult.errorMessage.contains("No query result returned from AAP API"))
    }

    @Test
    fun `should return failure when unexpected exception occurs`() {
      // Arrange
      val entityUuid = UUID.randomUUID()
      val timestamp = LocalDateTime.now()

      `when`(webClient.post()).thenReturn(requestBodyUriSpec)
      `when`(requestBodyUriSpec.uri("/query")).thenReturn(requestBodySpec)
      `when`(requestBodySpec.body(any())).thenReturn(requestHeadersSpec)
      `when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      `when`(responseSpec.bodyToMono(QueriesResponse::class.java))
        .thenReturn(Mono.error(RuntimeException("Unexpected error")))

      // Act
      val result = aapApi.fetchAssessment(entityUuid, timestamp)

      // Assert
      assertTrue(result is AAPApi.ApiOperationResult.Failure)
      val failureResult = result as AAPApi.ApiOperationResult.Failure
      assertTrue(failureResult.errorMessage.contains("Unexpected error during fetchAssessment"))
    }
  }
}

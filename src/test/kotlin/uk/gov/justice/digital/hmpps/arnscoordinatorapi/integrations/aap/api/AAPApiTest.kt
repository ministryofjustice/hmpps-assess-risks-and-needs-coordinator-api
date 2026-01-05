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
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.CreateAssessmentCommandResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
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
      endpoints = AAPApiProperties.Endpoints(command = "/command"),
    )
    aapApi = AAPApi(webClient, apiProperties)
  }

  @Nested
  inner class CreateAssessment {

    @Test
    fun `should return success when AAP API returns a valid response`() {
      val assessmentUuid = UUID.randomUUID()
      val createPlanData = CreatePlanData(PlanType.PLAN_ONLY, UserDetails("user-id", "User Name"))
      val response = CreateAssessmentCommandResult(
        type = "CreateAssessmentCommandResult",
        assessmentUuid = assessmentUuid.toString(),
      )

      `when`(webClient.post()).thenReturn(requestBodyUriSpec)
      `when`(requestBodyUriSpec.uri("/command")).thenReturn(requestBodySpec)
      `when`(requestBodySpec.body(any())).thenReturn(requestHeadersSpec)
      `when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      `when`(responseSpec.bodyToMono(CreateAssessmentCommandResult::class.java)).thenReturn(Mono.just(response))

      val result = aapApi.createAssessment(createPlanData)

      assertTrue(result is AAPApi.ApiOperationResult.Success)
      val successResult = result as AAPApi.ApiOperationResult.Success
      assertEquals(assessmentUuid, successResult.data.id)
      assertEquals(0, successResult.data.version)
      assertEquals(EntityType.AAP_PLAN, successResult.data.entityType)
    }

    @Test
    fun `should return failure when AAP API returns HTTP error`() {
      val createPlanData = CreatePlanData(PlanType.PLAN_ONLY, UserDetails("user-id", "User Name"))

      `when`(webClient.post()).thenReturn(requestBodyUriSpec)
      `when`(requestBodyUriSpec.uri("/command")).thenReturn(requestBodySpec)
      `when`(requestBodySpec.body(any())).thenReturn(requestHeadersSpec)
      `when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      `when`(responseSpec.bodyToMono(CreateAssessmentCommandResult::class.java))
        .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.BAD_REQUEST.value(), "Bad Request", null, "Error body".toByteArray(), null)))

      val result = aapApi.createAssessment(createPlanData)

      assertTrue(result is AAPApi.ApiOperationResult.Failure)
      val failureResult = result as AAPApi.ApiOperationResult.Failure
      assertTrue(failureResult.errorMessage.contains("HTTP error during create AAP assessment"))
    }

    @Test
    fun `should return failure when unexpected exception occurs`() {
      val createPlanData = CreatePlanData(PlanType.PLAN_ONLY, UserDetails("user-id", "User Name"))

      `when`(webClient.post()).thenReturn(requestBodyUriSpec)
      `when`(requestBodyUriSpec.uri("/command")).thenReturn(requestBodySpec)
      `when`(requestBodySpec.body(any())).thenReturn(requestHeadersSpec)
      `when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)
      `when`(responseSpec.bodyToMono(CreateAssessmentCommandResult::class.java))
        .thenReturn(Mono.error(RuntimeException("Unexpected error")))

      val result = aapApi.createAssessment(createPlanData)

      assertTrue(result is AAPApi.ApiOperationResult.Failure)
      val failureResult = result as AAPApi.ApiOperationResult.Failure
      assertTrue(failureResult.errorMessage.contains("Unexpected error during createAssessment"))
    }
  }
}

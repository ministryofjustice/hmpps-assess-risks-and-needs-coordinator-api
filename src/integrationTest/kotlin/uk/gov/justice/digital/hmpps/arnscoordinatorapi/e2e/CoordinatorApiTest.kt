package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.createOasys.CreateOasysHelper.executeCreateOasys
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.entity.GetEntityVersionsHelper.executeGetEntityVersionsNotFound
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.entity.GetEntityVersionsHelper.executeGetEntityVersionsOk
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanState
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

@DisplayName("Coordinator API Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoordinatorApiTest : IntegrationTestBase() {
  private lateinit var sentencePlanId: UUID
  val crn = Random.nextInt(0, 100000).toString().padStart(5, '0')
  val oasysPk = Random.nextInt(0, 1_000_000_000).toString()
  val oasysGenericRequest = OasysGenericRequest(
    userDetails = OasysUserDetails(name = "Test"),
  )
  val today = LocalDate.now()

  @BeforeAll
  fun `create oasys and get entity`() {
    val createOasysResponse = executeCreateOasys(webTestClient, crn, oasysPk)
    assertThat(createOasysResponse?.sentencePlanId).isNotNull
    sentencePlanId = createOasysResponse?.sentencePlanId!!
    val getEntityResponse = webTestClient.get().uri("/entity/${sentencePlanId}/AAP_PLAN")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysGetResponse>()
      .returnResult().responseBody
    assertThat(getEntityResponse?.planType).isEqualTo(PlanType.INITIAL)
    assertThat(getEntityResponse?.planComplete).isEqualTo(PlanState.INCOMPLETE)
  }

  @Test
  @Order(1)
  fun `lock plan`() {
    val lockApiResponse = webTestClient.post().uri("/oasys/${oasysPk}/lock")
      .bodyValue(oasysGenericRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody
    assertThat(lockApiResponse?.sanAssessmentVersion).isEqualTo(0)
    val getEntityVersionsResponse = executeGetEntityVersionsOk(webTestClient, sentencePlanId)
    assertThat(getEntityVersionsResponse?.allVersions?.get(today)?.planVersion?.status).isEqualTo("LOCKED")
  }

  @Test
  @Order(2)
  fun `soft-delete plan`() {
    val softDeleteApiResponse = webTestClient.post().uri("/oasys/${oasysPk}/soft-delete")
      .bodyValue(oasysGenericRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody
    assertThat(softDeleteApiResponse?.sanAssessmentVersion).isEqualTo(0)
    val getEntityVersionsResponse = executeGetEntityVersionsNotFound(webTestClient, sentencePlanId)
    assertThat(getEntityVersionsResponse).isEqualTo("No associations found for the provided entityUuid")
  }

  @Test
  @Order(3)
  fun `undelete plan`() {
    val undeleteApiResponse = webTestClient.post().uri("/oasys/${oasysPk}/undelete")
      .bodyValue(oasysGenericRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody
    assertThat(undeleteApiResponse?.sanAssessmentVersion).isEqualTo(0)
    val getEntityVersionsResponse = executeGetEntityVersionsOk(webTestClient, sentencePlanId)
    assertThat(getEntityVersionsResponse?.allVersions?.get(today)?.planVersion?.status).isEqualTo("LOCKED")
  }
}

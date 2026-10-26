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
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.UpdateCommandHelper.postUpdateCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.createOasys.CreateOasysHelper.createOasys
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.entity.GetEntityVersionsHelper.getEntityVersions
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.entity.GetEntityVersionsHelper.getEntityVersionsNotFound
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.query.QueryApiHelper.getQueryResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

@DisplayName("Coordinator API Tests - SAN")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoordinatorApiSanTest : IntegrationTestBase() {
  private lateinit var sanAssessmentUuid: UUID

  val crn = Random.nextInt(0, 100000).toString().padStart(5, '0')
  val oasysPk = Random.nextInt(0, 1_000_000_000).toString()
  val oasysGenericRequest = OasysGenericRequest(
    userDetails = OasysUserDetails(name = "Test"),
  )
  val today = LocalDate.now()

  @BeforeAll
  fun `create oasys and get entity`() {
    val createOasysResponse = createOasys(webTestClient, crn, oasysPk)

    assertThat(createOasysResponse?.sanAssessmentId).isNotNull

    sanAssessmentUuid = createOasysResponse?.sanAssessmentId!!

    val getEntityResponse = webTestClient.get().uri("/entity/$sanAssessmentUuid/AAP_SAN")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysGetResponse>()
      .returnResult().responseBody

    assertThat(getEntityResponse?.sanAssessmentData).isNotNull
    postUpdateCommand(aapTestClient, sanAssessmentUuid)
  }

  @Test
  @Order(1)
  fun `soft-delete assessment`() {
    webTestClient.post().uri("/oasys/$oasysPk/soft-delete")
      .bodyValue(oasysGenericRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody

    val getEntityVersionsResponse = getEntityVersionsNotFound(webTestClient, sanAssessmentUuid)
    assertThat(getEntityVersionsResponse).isEqualTo("No associations found for the provided entityUuid")
  }

  @Test
  @Order(2)
  fun `undelete assessment`() {
    webTestClient.post().uri("/oasys/$oasysPk/undelete")
      .bodyValue(oasysGenericRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody
  }

  @Test
  @Order(3)
  fun `query assessment`() {
    val queryResponse = getQueryResponse(aapTestClient, sanAssessmentUuid)
    val assessmentVersion = queryResponse?.queries[0]?.result as AssessmentVersionQueryResult
    assertThat(assessmentVersion.answers.keys).isNotEmpty()
  }

  @Test
  @Order(4)
  fun `get entity versions`() {
    val getEntityVersionsResponse = getEntityVersions(webTestClient, sanAssessmentUuid)
    assertThat(getEntityVersionsResponse?.allVersions?.get(today)?.assessmentVersion?.status).isEqualTo("UNSIGNED")
  }
}

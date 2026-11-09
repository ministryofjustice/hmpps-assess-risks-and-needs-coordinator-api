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
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.SingleValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.SanAssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.oasysdata.OasysData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
  val today = LocalDate.now(ZoneId.of("UTC"))

  private val createdQ3p4 = "2"
  private val linkedQ3p4 = "0"

  @BeforeAll
  fun `create oasys and get entity`() {
    val createOasysResponse = createOasys(
      webTestClient,
      crn,
      oasysPk,
      sanAssessmentType = SanAssessmentType.COMMUNITY,
      oasysData = OasysData(q3p4SuitabilityOfAccommodation = createdQ3p4, q8p1DrugsEverMisused = "YES"),
    )

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
    assertThat(getEntityVersionsResponse?.unaggregatedVersions)
      .anyMatch { it.status == "UNSIGNED" }
  }

  @Test
  @Order(5)
  fun `created assessment persists SAN assessment type, created timestamp and prefixed OASys data`() {
    val properties = (getQueryResponse(aapTestClient, sanAssessmentUuid)?.queries[0]?.result as AssessmentVersionQueryResult).properties

    assertThat(properties["SAN_ASSESSMENT_TYPE"]).isEqualTo(SingleValue("COMMUNITY"))
    assertThat(properties["OASYS_DATA_o3-4"]).isEqualTo(SingleValue("2"))
    assertThat(properties["OASYS_DATA_o8-1"]).isEqualTo(SingleValue("YES"))
    assertThat(Instant.parse((properties["OASYS_CREATED"] as SingleValue).value)).isBeforeOrEqualTo(Instant.now())
    assertThat(properties).doesNotContainKey("OASYS_LINKED")
  }

  @Test
  @Order(6)
  fun `linked assessment persists linked timestamp and new SAN assessment type and ignores OASys data`() {
    createOasys(
      webTestClient,
      crn,
      Random.nextInt(0, 1_000_000_000).toString(),
      previousOasysSanPk = oasysPk,
      sanAssessmentType = SanAssessmentType.CUSTODY,
      oasysData = OasysData(q3p4SuitabilityOfAccommodation = linkedQ3p4),
    )

    val properties = (getQueryResponse(aapTestClient, sanAssessmentUuid)?.queries[0]?.result as AssessmentVersionQueryResult).properties
    val createdAt = Instant.parse((properties["OASYS_CREATED"] as SingleValue).value)
    val linkedAt = Instant.parse((properties["OASYS_LINKED"] as SingleValue).value)

    assertThat(properties["SAN_ASSESSMENT_TYPE"]).isEqualTo(SingleValue("CUSTODY"))
    assertThat(linkedAt).isAfterOrEqualTo(createdAt)
    assertThat(properties["OASYS_DATA_o3-4"])
      .isEqualTo(SingleValue(createdQ3p4))
      .isNotEqualTo(SingleValue(linkedQ3p4))
  }
}

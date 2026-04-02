package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

class SignTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @Autowired
  lateinit var oasysVersionRepository: OasysVersionRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsSign()
  }

  @Test
  fun `it successfully signs an existing SP and SAN for an oasys PK`() {
    val oasysAssessmentPk = getRandomOasysPk()
    val planUuid = UUID.randomUUID()
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.AAP_PLAN,
          entityUuid = planUuid,
        ),
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
      ),
    )

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysSignRequest(
          signType = SignType.COUNTERSIGN,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody(OasysVersionedEntityResponse::class.java)
      .returnResult()
      .responseBody

    val planVersions = oasysVersionRepository.findAllByEntityUuid(planUuid)

    assertThat(response?.sanAssessmentId).isEqualTo(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
    assertThat(response?.sanAssessmentVersion).isEqualTo(0)
    assertThat(response?.sentencePlanId).isEqualTo(planUuid)
    assertThat(planVersions).hasSize(1)
    assertThat(planVersions.first().createdBy).isEqualTo(OasysEvent.AWAITING_COUNTERSIGN)
    assertThat(response?.sentencePlanVersion).isEqualTo(planVersions.first().version)
  }

  @Test
  fun `it returns a 409 when the SAN assessment is already signed`() {
    stubAssessmentsSign(409)

    val oasysAssessmentPk = getRandomOasysPk()
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
      ),
    )

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysSignRequest(
          signType = SignType.COUNTERSIGN,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("Failed to sign ASSESSMENT entity due to a conflict, Assessment already signed")
  }

  @Test
  fun `it successfully signs an existing sentence plan without SAN`() {
    val oasysAssessmentPk = getRandomOasysPk()
    val planUuid = UUID.randomUUID()
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.AAP_PLAN,
          entityUuid = planUuid,
        ),
      ),
    )

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysSignRequest(
          signType = SignType.COUNTERSIGN,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody(OasysVersionedEntityResponse::class.java)
      .returnResult()
      .responseBody

    val planVersions = oasysVersionRepository.findAllByEntityUuid(planUuid)

    assertThat(response?.sanAssessmentId).isEqualTo(UUID(0, 0))
    assertThat(response?.sentencePlanId).isEqualTo(planUuid)
    assertThat(planVersions).hasSize(1)
    assertThat(planVersions.first().createdBy).isEqualTo(OasysEvent.AWAITING_COUNTERSIGN)
    assertThat(response?.sentencePlanVersion).isEqualTo(planVersions.first().version)
  }

  @Test
  fun `it returns a 404 when no associations found`() {
    val request = OasysSignRequest(
      signType = SignType.COUNTERSIGN,
      userDetails = OasysUserDetails(id = "1", name = "Test Name"),
    )

    webTestClient.post().uri("/oasys/999/sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `it returns 400 when validation errors occur in the path parameter`() {
    val sixteenCharPk = "012345678901234A"

    val response = webTestClient.post().uri("/oasys/$sixteenCharPk/sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysSignRequest(
          signType = SignType.COUNTERSIGN,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - size must be between 1 and 15")
  }
}

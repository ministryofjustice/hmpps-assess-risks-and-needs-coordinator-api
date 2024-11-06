package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UndeleteData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.*

class UndeleteTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsUndelete()
    stubSentencePlanUndelete()
  }

  @Test
  fun `it successfully undeletes an existing SP and SAN for an oasys PK`() {
    val oasysAssessmentPk = "699"
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          id = 699L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa6"),
          deleted = true,
        ),
        OasysAssociation(
          id = 700L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
          deleted = true,
        ),
      ),
    )
    val versionsBefore = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk).size
    assertThat(versionsBefore).isEqualTo(0)

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/undelete")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        UndeleteData(
          versionFrom = 0,
          userDetails = UserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody(OasysGetResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.sanAssessmentId).isEqualTo(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
    assertThat(response?.sanAssessmentVersion).isEqualTo(0)

    assertThat(response?.sentencePlanId).isEqualTo(UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa6"))
    assertThat(response?.sentencePlanVersion).isEqualTo(1)

    val versionsAfter = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk).size
    assertThat(versionsAfter).isEqualTo(2)
  }

  @Test
  fun `it returns a 409 when the SAN assessment is already undeleted`() {
    stubAssessmentsUndelete(409)
    val oasysAssessmentPk = "600"
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          id = 1L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
          deleted = true,
        ),
      ),
    )

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/undelete")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        UndeleteData(
          versionFrom = 0,
          userDetails = UserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).startsWith("Failed to undelete ASSESSMENT versions from 0 to null due to a conflict")
  }

  @Test
  fun `it returns a 409 when the Sentence Plan is already undeleted`() {
    stubSentencePlanUndelete(409)
    val oasysAssessmentPk = "601"
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          id = 601L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
          deleted = true,
        ),
      ),
    )

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/undelete")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        UndeleteData(
          versionFrom = 0,
          userDetails = UserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).startsWith("Failed to undelete PLAN versions from 0 to null due to a conflict")
  }

  @Test
  fun `it returns a 404 when no deleted associations found`() {
    val oasysAssessmentPk = "401"
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          id = 401L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
          deleted = false,
        ),
      ),
    )
    webTestClient.post().uri("/oasys/999/undelete")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        UndeleteData(
          versionFrom = 0,
          userDetails = UserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `it returns 400 when validation errors occur in the path parameter`() {
    val sixteenCharPk = "012345678901234A"

    val response = webTestClient.post().uri("/oasys/$sixteenCharPk/undelete")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysGenericRequest(
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

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.*
class RollbackTest : IntegrationTestBase() {
  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository
  lateinit var oasysAssessmentPk: String

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsRollback()
    stubSentencePlanRollback()
    oasysAssessmentPk = getRandomOasysPk()

    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("4da85f64-5717-4562-b3fc-2c963f66afb8"),
        ),
      ),
    )
  }

  fun request(oasysPk: String) = webTestClient.post().uri("/oasys/$oasysPk/rollback")
    .header(HttpHeaders.CONTENT_TYPE, "application/json")
    .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
    .bodyValue(
      OasysRollbackRequest(
        sanVersionNumber = 1,
        sentencePlanVersionNumber = 2,
        userDetails = OasysUserDetails(id = "1", name = "Test Name"),
      ),
    )
    .exchange()

  @Test
  fun `it successfully rolls back an existing SP and SAN for an oasys PK`() {
    val response = request(oasysAssessmentPk)
      .expectStatus().isOk
      .expectBody(OasysVersionedEntityResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.sanAssessmentId).isEqualTo(UUID.fromString("4da85f64-5717-4562-b3fc-2c963f66afb8"))
    assertThat(response?.sanAssessmentVersion).isEqualTo(1)
    assertThat(response?.sentencePlanId).isEqualTo(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
    assertThat(response?.sentencePlanVersion).isEqualTo(2)
  }

  @Test
  fun `it returns a 409 when the SAN assessment is already rolled back`() {
    stubAssessmentsRollback(409)

    val response = request(oasysAssessmentPk)
      .expectStatus().isEqualTo(409)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("Failed to roll back ASSESSMENT entity due to a conflict, Unable to roll back this assessment version")
  }

  @Test
  fun `it returns a 409 when the Sentence Plan is already locked`() {
    stubSentencePlanRollback(409)

    val response = request(oasysAssessmentPk)
      .expectStatus().isEqualTo(409)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("Failed to roll back PLAN entity due to a conflict, Unable to roll back this plan version")
  }

  @Test
  fun `it returns a 404 when no associations found`() {
    request("999").expectStatus().isNotFound
  }

  @Test
  fun `it returns 400 when validation errors occur in the path parameter`() {
    val sixteenCharPk = "012345678901234A"

    val response = request(sixteenCharPk)
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - size must be between 1 and 15")
  }

  @Test
  fun `it returns 400 when validation errors occur in the body`() {
    val fifteenCharPk = "012345678901234"

    val response = webTestClient.post().uri("/oasys/$fifteenCharPk/rollback")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysRollbackRequest(
          sanVersionNumber = -1,
          sentencePlanVersionNumber = -2,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("oasysRollbackRequest.sentencePlanVersionNumber - must be greater than or equal to 0")
    assertThat(response.responseBody?.developerMessage).contains("oasysRollbackRequest.sanVersionNumber - must be greater than or equal to 0")
  }
}

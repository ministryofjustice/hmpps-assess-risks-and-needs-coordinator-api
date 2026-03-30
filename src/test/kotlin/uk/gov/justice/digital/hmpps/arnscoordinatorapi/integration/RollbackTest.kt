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
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

class RollbackTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @Autowired
  lateinit var oasysVersionRepository: OasysVersionRepository

  lateinit var oasysAssessmentPk: String
  lateinit var planUuid: UUID

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsRollback()
    oasysAssessmentPk = getRandomOasysPk()
    planUuid = UUID.randomUUID()

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
          entityUuid = UUID.fromString("4da85f64-5717-4562-b3fc-2c963f66afb8"),
        ),
      ),
    )
    oasysVersionRepository.save(
      OasysVersionEntity(
        createdBy = OasysEvent.AWAITING_COUNTERSIGN,
        entityUuid = planUuid,
        version = 2,
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

    val planVersion = oasysVersionRepository.findByEntityUuidAndVersion(
      planUuid,
      2,
    )

    assertThat(response?.sanAssessmentId).isEqualTo(UUID.fromString("4da85f64-5717-4562-b3fc-2c963f66afb8"))
    assertThat(response?.sanAssessmentVersion).isEqualTo(1)
    assertThat(response?.sentencePlanId).isEqualTo(planUuid)
    assertThat(response?.sentencePlanVersion).isEqualTo(2)
    assertThat(planVersion?.createdBy).isEqualTo(OasysEvent.ROLLED_BACK)
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
  fun `it returns a 500 when the sentence plan version does not exist`() {
    val oasysAssessmentPk = getRandomOasysPk()
    val planUuid = UUID.randomUUID()
    oasysAssociationRepository.save(
      OasysAssociation(
        oasysAssessmentPk = oasysAssessmentPk,
        entityType = EntityType.AAP_PLAN,
        entityUuid = planUuid,
      ),
    )

    val response = request(oasysAssessmentPk)
      .expectStatus().is5xxServerError
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("Failed to roll back AAP_PLAN entity, Unable to update version '2' for entity $planUuid")
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

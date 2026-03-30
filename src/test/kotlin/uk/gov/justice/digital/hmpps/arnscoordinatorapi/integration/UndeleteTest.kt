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
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

class UndeleteTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @Autowired
  lateinit var oasysVersionRepository: OasysVersionRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsUndelete()
  }

  @Test
  fun `it successfully undeletes an existing SP and SAN for an oasys PK`() {
    val oasysAssessmentPk = getRandomOasysPk()
    val planUuid = UUID.randomUUID()
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.AAP_PLAN,
          entityUuid = planUuid,
          deleted = true,
        ),
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
          deleted = true,
        ),
      ),
    )
    oasysVersionRepository.save(
      OasysVersionEntity(
        createdBy = OasysEvent.LOCKED,
        entityUuid = planUuid,
        version = 0,
        deleted = true,
      ),
    )

    val versionsBefore = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk).size

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
      .expectBody(OasysVersionedEntityResponse::class.java)
      .returnResult()
      .responseBody

    val versionsAfter = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk).size
    val planVersion = oasysVersionRepository.findByEntityUuidAndVersion(planUuid, 0)

    assertThat(versionsBefore).isEqualTo(0)
    assertThat(response?.sanAssessmentId).isEqualTo(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
    assertThat(response?.sanAssessmentVersion).isEqualTo(0)
    assertThat(response?.sentencePlanId).isEqualTo(planUuid)
    assertThat(response?.sentencePlanVersion).isEqualTo(0)
    assertThat(versionsAfter).isEqualTo(2)
    assertThat(planVersion?.deleted).isFalse()
  }

  @Test
  fun `it returns a 409 when the SAN assessment is already undeleted`() {
    stubAssessmentsUndelete(409)

    val oasysAssessmentPk = getRandomOasysPk()
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
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
  fun `it returns a 500 when the sentence plan has no deleted versions to undelete`() {
    val oasysAssessmentPk = getRandomOasysPk()
    val planUuid = UUID.randomUUID()
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.AAP_PLAN,
          entityUuid = planUuid,
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
      .expectStatus().is5xxServerError
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("Failed to undelete association for $oasysAssessmentPk, Something went wrong while un-deleting versions for entity $planUuid")
  }

  @Test
  fun `it returns a 404 when no deleted associations found`() {
    val oasysAssessmentPk = getRandomOasysPk()
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.AAP_PLAN,
          entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
          deleted = false,
        ),
      ),
    )

    webTestClient.post().uri("/oasys/${getRandomOasysPk()}/undelete")
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

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysAssociationsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysAssociationEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.repository.OasysVersionRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class AssociationsTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @Autowired
  lateinit var oasysVersionRepository: OasysVersionRepository

  private fun givenAssociations(oasysAssessmentPk: String, planUuid: UUID, sanUuid: UUID) {
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociationEntity(oasysAssessmentPk = oasysAssessmentPk, entityType = EntityType.AAP_PLAN, entityUuid = planUuid),
        OasysAssociationEntity(oasysAssessmentPk = oasysAssessmentPk, entityType = EntityType.AAP_SAN, entityUuid = sanUuid),
      ),
    )
  }

  @Test
  fun `it returns the associated entity ids`() {
    val oasysAssessmentPk = getRandomOasysPk()
    val planUuid = UUID.randomUUID()
    val sanUuid = UUID.randomUUID()
    givenAssociations(oasysAssessmentPk, planUuid, sanUuid)

    val response = webTestClient.get().uri("/oasys/$oasysAssessmentPk/associations")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysAssociationsResponse>()
      .returnResult()
      .responseBody

    assertThat(response).isEqualTo(OasysAssociationsResponse(sanAssessmentId = sanUuid, sentencePlanId = planUuid))
  }

  @Test
  fun `it translates the requested version numbers to version timestamps`() {
    val oasysAssessmentPk = getRandomOasysPk()
    val planUuid = UUID.randomUUID()
    val sanUuid = UUID.randomUUID()
    givenAssociations(oasysAssessmentPk, planUuid, sanUuid)
    val planCreatedAt = LocalDateTime.parse("2026-01-01T10:00:00")
    val sanCreatedAt = LocalDateTime.parse("2026-01-02T11:00:00")
    oasysVersionRepository.saveAll(
      listOf(
        OasysVersionEntity(createdBy = OasysEvent.CREATED, version = 5, entityUuid = planUuid, createdAt = planCreatedAt),
        OasysVersionEntity(createdBy = OasysEvent.CREATED, version = 7, entityUuid = sanUuid, createdAt = sanCreatedAt),
      ),
    )

    val response = webTestClient.get().uri("/oasys/$oasysAssessmentPk/associations?planVersion=5&assessmentVersion=7")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysAssociationsResponse>()
      .returnResult()
      .responseBody

    assertThat(response?.sentencePlanVersion).isEqualTo(planCreatedAt.toInstant(ZoneOffset.UTC).toEpochMilli())
    assertThat(response?.sanAssessmentVersion).isEqualTo(sanCreatedAt.toInstant(ZoneOffset.UTC).toEpochMilli())
  }

  @Test
  fun `it returns 500 when a requested version does not exist`() {
    val oasysAssessmentPk = getRandomOasysPk()
    givenAssociations(oasysAssessmentPk, UUID.randomUUID(), UUID.randomUUID())

    webTestClient.get().uri("/oasys/$oasysAssessmentPk/associations?planVersion=5")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().is5xxServerError
  }

  @Test
  fun `it returns 404 when there are no associations`() {
    webTestClient.get().uri("/oasys/${getRandomOasysPk()}/associations")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `it returns 400 when validation errors occur in the path parameter`() {
    val sixteenCharPk = "012345678901234A"

    val response = webTestClient.get().uri("/oasys/$sixteenCharPk/associations")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - size must be between 1 and 15")
  }
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import java.util.UUID

class EntityGetTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsGet()
    stubSentencePlanGet()
  }

  @Test
  fun `it successfully gets an ASSESSMENT or SAN or for an oasys PK linked to a given sp id`() {
    val oasysAssessmentPk = getRandomOasysPk()
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("2fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
      ),
    )

    val assessmentResponse = webTestClient.get().uri("/entity/5fa85f64-5717-4562-b3fc-2c963f66afa6/ASSESSMENT")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isEqualTo(200)
      .expectBody(OasysGetResponse::class.java)
      .returnResult()
      .responseBody

    val sentencePlanResponse = webTestClient.get().uri("/entity/5fa85f64-5717-4562-b3fc-2c963f66afa6/PLAN")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isEqualTo(200)
      .expectBody(OasysGetResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(assessmentResponse?.sentencePlanId).isNull()
    assertThat(assessmentResponse?.sanAssessmentId).isNotNull()
    assertThat(assessmentResponse?.sanAssessmentVersion).isEqualTo(1)
    assertThat(assessmentResponse?.sanAssessmentData?.get("q2")).hasFieldOrPropertyWithValue("value", "Question answer &, ', <, >, /, \\, `, \"")
    assertThat(assessmentResponse?.sanOasysEquivalent).isEqualTo(mapOf("q2" to "Question answer &, ', <, >, /, \\, `, \""))

    assertThat(sentencePlanResponse?.sanAssessmentId).isNull()
    assertThat(sentencePlanResponse?.sentencePlanId).isNotNull()
  }

  @Test
  fun `get by entity id returns bad request found`() {
    webTestClient.get().uri("/entity/5dc85f64-5717-4562-b3fc-2c963f66afa7/INVALID")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isEqualTo(400)
  }

  @Test
  fun `get by entity id returns not found`() {
    webTestClient.get().uri("/entity/5dc85f64-5717-4562-b3fc-2c963f66afa7/PLAN")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isEqualTo(404)
  }
}

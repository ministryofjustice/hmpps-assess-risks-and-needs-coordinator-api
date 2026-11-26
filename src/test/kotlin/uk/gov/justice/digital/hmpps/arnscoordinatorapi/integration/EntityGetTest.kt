package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysAssociationEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.repository.OasysAssociationRepository
import java.util.UUID

class EntityGetTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAAPQueryAssessment(200, "SENTENCE_PLAN", UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"))
    stubAAPQueryAssessment(200, "STRENGTHS_AND_NEEDS", UUID.fromString("2fa85f64-5717-4562-b3fc-2c963f66afa6"))
  }

  @Test
  fun `it only gets the requested entity type for the oasys PK linked to a given entity id`() {
    val oasysAssessmentPk = getRandomOasysPk()
    val (planAssociation, sanAssociation) = oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociationEntity(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.AAP_PLAN,
          entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
        OasysAssociationEntity(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.AAP_SAN,
          entityUuid = UUID.fromString("2fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
      ),
    )

    val sentencePlanResponse = webTestClient.get().uri("/entity/${planAssociation.entityUuid}/AAP_PLAN")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isEqualTo(200)
      .expectBody(OasysGetResponse::class.java)
      .returnResult()
      .responseBody

    val assessmentResponse = webTestClient.get().uri("/entity/${sanAssociation.entityUuid}/AAP_SAN")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isEqualTo(200)
      .expectBody(OasysGetResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(assessmentResponse?.sentencePlanId).isEqualTo(UUID(0, 0))
    assertThat(assessmentResponse?.sanAssessmentId).isEqualTo(sanAssociation.entityUuid)

    assertThat(sentencePlanResponse?.sanAssessmentId).isEqualTo(UUID(0, 0))
    assertThat(sentencePlanResponse?.sentencePlanId).isEqualTo(planAssociation.entityUuid)

    // Each request only queries AAP for the requested entity
    assertThat(aap.queriesSent().map { it.path("assessmentIdentifier").path("uuid").asText() })
      .containsExactly(planAssociation.entityUuid.toString(), sanAssociation.entityUuid.toString())
  }

  @Test
  fun `get by entity id returns bad request for an unsupported entity type`() {
    webTestClient.get().uri("/entity/5dc85f64-5717-4562-b3fc-2c963f66afa7/INVALID")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isEqualTo(400)
  }

  @Test
  fun `get by entity id returns not found`() {
    webTestClient.get().uri("/entity/5dc85f64-5717-4562-b3fc-2c963f66afa7/AAP_PLAN")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isEqualTo(404)
  }
}

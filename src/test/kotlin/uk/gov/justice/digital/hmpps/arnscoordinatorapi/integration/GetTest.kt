package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import java.util.UUID

class GetTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsGet()
    stubSentencePlanGet()
  }

  @Test
  fun `it successfully gets an existing SP and SAN for an oasys PK`() {
    val oasysAssessmentPk = "1"
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          id = 1L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
        OasysAssociation(
          id = 2L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
      ),

    )

    val response = webTestClient.get().uri("/oasys/1")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isEqualTo(200)
      .expectBody(OasysGetResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.sanAssessmentId).isEqualTo(UUID.fromString("11db45b5-215d-4405-a887-a7efd5216fa2"))
    assertThat(response?.sanAssessmentVersion).isEqualTo(1)
    assertThat(response?.sanAssessmentData?.get("q2")).hasFieldOrPropertyWithValue("value", "Question answer &, ', <, >, /, \\, `, \"")
    assertThat(response?.sanOasysEquivalent).isEqualTo(mapOf("q2" to "Question answer &, ', <, >, /, \\, `, \""))
  }

  @Test
  fun `it successfully gets an existing SP and SAN for an oasys PK linked to a given entity id`() {
    val oasysAssessmentPk = "12345"
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          id = 1L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
        OasysAssociation(
          id = 2L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("2fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
      ),
    )

    val spResponse = webTestClient.get().uri("/oasys/entity/5fa85f64-5717-4562-b3fc-2c963f66afa6")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isEqualTo(200)
      .expectBody(OasysGetResponse::class.java)
      .returnResult()
      .responseBody

    val sanResponse = webTestClient.get().uri("/oasys/entity/2fa85f64-5717-4562-b3fc-2c963f66afa6")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isEqualTo(200)
      .expectBody(OasysGetResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(spResponse?.sanAssessmentId).isNotNull()
    assertThat(spResponse?.sanAssessmentVersion).isEqualTo(1)
    assertThat(spResponse?.sanAssessmentData?.get("q2")).hasFieldOrPropertyWithValue("value", "Question answer &, ', <, >, /, \\, `, \"")
    assertThat(spResponse?.sanOasysEquivalent).isEqualTo(mapOf("q2" to "Question answer &, ', <, >, /, \\, `, \""))

    assertThat(sanResponse?.sanAssessmentId).isNotNull()
    assertThat(sanResponse?.sanAssessmentVersion).isEqualTo(1)
    assertThat(sanResponse?.sanAssessmentData?.get("q2")).hasFieldOrPropertyWithValue("value", "Question answer &, ', <, >, /, \\, `, \"")
    assertThat(sanResponse?.sanOasysEquivalent).isEqualTo(mapOf("q2" to "Question answer &, ', <, >, /, \\, `, \""))
  }

  @Test
  fun `get by entity id returns not found`() {
    webTestClient.get().uri("/oasys/entity/5dc85f64-5717-4562-b3fc-2c963f66afa7")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(404)
  }
}

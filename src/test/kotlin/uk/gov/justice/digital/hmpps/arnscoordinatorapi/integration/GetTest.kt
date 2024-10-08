package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import java.util.*

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
    assertThat(response?.sanOasysEquivalent).isEqualTo(mapOf("q2" to "2"))
  }
}

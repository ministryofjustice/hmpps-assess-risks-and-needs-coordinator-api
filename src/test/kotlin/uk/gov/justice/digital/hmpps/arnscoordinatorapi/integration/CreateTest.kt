package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.util.*

class CreateTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsCreate()
    stubSentencePlanCreate()
  }

  @Test
  fun `it successfully creates a new SP and SAN with no previous oasys PK`() {
    val oasysAssessmentPk = "1"
    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isCreated
    val associations = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk)
    val sentencePlanAssociation = associations.firstOrNull { it.entityType == EntityType.PLAN }
    val sanAssociation = associations.firstOrNull { it.entityType == EntityType.ASSESSMENT }
    assertThat(sentencePlanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(sanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(sentencePlanAssociation?.entityUuid).isEqualTo(UUID.fromString("4180ed3e-2412-4ca5-9b30-9add00941113"))
    assertThat(sanAssociation?.entityUuid).isEqualTo(UUID.fromString("90a71d16-fecd-4e1a-85b9-98178bf0f8d0"))
  }

  @Test
  fun `it returns a conflict status where an association already exists with the oasys PK`() {
    val oasysAssessmentPk = "2"
    oasysAssociationRepository.save(
      OasysAssociation(
        id = 1L,
        oasysAssessmentPk = oasysAssessmentPk,
        entityUuid = UUID.randomUUID(),
        entityType = EntityType.ASSESSMENT,
      ),
    )
    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isEqualTo(409)
  }

  @Test
  fun `it returns a 500 status where a call to the downstream sentence plan service returns 500`() {
    stubSentencePlanCreate(500)
    val oasysAssessmentPk = "3"
    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isEqualTo(500)
  }

  @Test
  fun `it returns a 400 status where no oasysAssessmentPk provided`() {
    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isBadRequest
  }
}

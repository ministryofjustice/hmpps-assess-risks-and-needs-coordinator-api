package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.CoordinatorEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.EventType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.OasysEventPublisher
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.VersionPayload
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

class CreateTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @MockitoBean
  lateinit var oasysEventPublisher: OasysEventPublisher

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsCreate()
    stubAAPCreateAssessment()
    stubAssessmentsClone()
    doNothing().whenever(oasysEventPublisher).publish(any())
  }

  @Test
  fun `it successfully creates a new SP and SAN with no previous oasys PK`() {
    val oasysAssessmentPk = getRandomOasysPk()
    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isCreated
    val associations = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk)
    val aapPlanAssociation = associations.firstOrNull { it.entityType == EntityType.AAP_PLAN }
    val sanAssociation = associations.firstOrNull { it.entityType == EntityType.ASSESSMENT }
    assertThat(aapPlanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(sanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(aapPlanAssociation?.entityUuid).isEqualTo(UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"))
    assertThat(sanAssociation?.entityUuid).isEqualTo(UUID.fromString("90a71d16-fecd-4e1a-85b9-98178bf0f8d0"))
  }

  @Test
  fun `it publishes an OASYS_VERSION_EVENT when create succeeds`() {
    val oasysAssessmentPk = getRandomOasysPk()

    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          regionPrisonCode = "MDI",
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isCreated

    val captor = argumentCaptor<CoordinatorEvent>()
    verify(oasysEventPublisher, times(1)).publish(captor.capture())

    val event = captor.firstValue
    assertThat(event.eventType).isEqualTo(EventType.OASYS_VERSION_EVENT)
    assertThat(event.entityType).isEqualTo("AAP_PLAN")
    assertThat(event.message).isInstanceOf(VersionPayload::class.java)

    val payload = event.message as VersionPayload
    assertThat(payload.oasysEvent).isEqualTo(OasysEvent.CREATED)
    assertThat(payload.deleted).isFalse()
    assertThat(payload.association.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(payload.association.regionPrisonCode).isEqualTo("MDI")
    assertThat(payload.association.baseVersion).isEqualTo(1L)
  }

  @Test
  fun `it returns a 500 status where a call to the downstream AAP service returns 500`() {
    stubAAPCreateAssessment(500)
    val oasysAssessmentPk = getRandomOasysPk()
    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isEqualTo(500)

    val associations = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk)
    assertThat(associations).isEmpty()
  }

  @Test
  fun `it does not publish an event when create fails`() {
    stubAAPCreateAssessment(500)
    val oasysAssessmentPk = getRandomOasysPk()

    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isEqualTo(500)

    verify(oasysEventPublisher, never()).publish(any())
  }

  @Test
  fun `it returns a 400 status where no oasysAssessmentPk provided`() {
    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_WRITE")))
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `it successfully links existing SP and SAN when previous PKs are supplied`() {
    val previousOasysPk = getRandomOasysPk()
    val oasysAssessmentPk = getRandomOasysPk()

    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = previousOasysPk,
          entityType = EntityType.AAP_PLAN,
          entityUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
        OasysAssociation(
          oasysAssessmentPk = previousOasysPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("90a71d16-fecd-4e1a-85b9-98178bf0f8d0"),
        ),
      ),
    )

    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          previousOasysSanPk = previousOasysPk,
          previousOasysSpPk = previousOasysPk,
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isCreated
    val associations = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk)
    val sentencePlanAssociation = associations.firstOrNull { it.entityType == EntityType.AAP_PLAN }
    val sanAssociation = associations.firstOrNull { it.entityType == EntityType.ASSESSMENT }
    assertThat(sentencePlanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(sanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(sentencePlanAssociation?.entityUuid).isEqualTo(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
    assertThat(sanAssociation?.entityUuid).isEqualTo(UUID.fromString("90a71d16-fecd-4e1a-85b9-98178bf0f8d0"))
  }

  @Test
  fun `it returns a conflict status where an association already exists with the oasys PK`() {
    val oasysAssessmentPk = getRandomOasysPk()

    oasysAssociationRepository.save(
      OasysAssociation(
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
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isEqualTo(409)
  }

  @Test
  fun `it returns a 404 not found when a previous oasys PK is supplied that does not have an association`() {
    val previousOasysPk = getRandomOasysPk()
    val oasysAssessmentPk = getRandomOasysPk()

    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          previousOasysSanPk = previousOasysPk,
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `it returns 400 when validation errors occur in the body`() {
    val invalidPk = "012345678901234A"

    val response = webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          previousOasysSanPk = invalidPk,
          oasysAssessmentPk = invalidPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("previousOasysSanPk - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("previousOasysSanPk - size must be between 1 and 15")
    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPk - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPk - size must be between 1 and 15")
  }
}

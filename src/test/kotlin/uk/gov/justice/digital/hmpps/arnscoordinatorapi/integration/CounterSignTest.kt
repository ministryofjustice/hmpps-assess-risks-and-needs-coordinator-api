package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Constraints
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

class CounterSignTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @Autowired
  lateinit var oasysVersionRepository: OasysVersionRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
  }

  @Test
  fun `it successfully countersigns an existing SP and SAN for an oasys PK`() {
    val oasysAssessmentPk = getRandomOasysPk()
    val (planAssociation, sanAssociation) = oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.AAP_PLAN,
          entityUuid = UUID.randomUUID(),
        ),
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.AAP_SAN,
          entityUuid = UUID.randomUUID(),
        ),
      ),
    )
    val (planVersion, sanVersion) = oasysVersionRepository.saveAll(
      listOf(
        OasysVersionEntity(
          createdBy = OasysEvent.AWAITING_COUNTERSIGN,
          entityUuid = planAssociation.entityUuid,
          version = 0,
        ),
        OasysVersionEntity(
          createdBy = OasysEvent.AWAITING_COUNTERSIGN,
          entityUuid = sanAssociation.entityUuid,
          version = 0,
        ),
      ),
    )

    val request = OasysCounterSignRequest(
      sanVersionNumber = sanVersion.version,
      sentencePlanVersionNumber = planVersion.version,
      outcome = CounterSignOutcome.COUNTERSIGNED,
      userDetails = OasysUserDetails(id = "1", name = "Test Name"),
    )

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody(OasysVersionedEntityResponse::class.java)
      .returnResult()
      .responseBody

    val updatedSanVersion =
      oasysVersionRepository.findByEntityUuidAndVersion(sanAssociation.entityUuid, sanVersion.version)

    assertThat(response?.sanAssessmentId).isEqualTo(sanAssociation.entityUuid)
    assertThat(response?.sanAssessmentVersion).isEqualTo(sanVersion.version)
    assertThat(response?.sentencePlanId).isEqualTo(planAssociation.entityUuid)
    assertThat(response?.sentencePlanVersion).isEqualTo(planVersion.version)
    assertThat(updatedSanVersion?.createdBy).isEqualTo(OasysEvent.COUNTERSIGNED)
  }

//  @Test
//  fun `it returns a 409 when the SAN assessment is already locked`() {
//    val oasysAssessmentPk = getRandomOasysPk()
//    val association = oasysAssociationRepository.save(
//      OasysAssociation(
//        oasysAssessmentPk = oasysAssessmentPk,
//        entityType = EntityType.AAP_SAN,
//        entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
//      ),
//    )
//
//    val countersignedVersion = oasysVersionRepository.save(
//      OasysVersionEntity(
//        createdBy = OasysEvent.COUNTERSIGNED,
//        version = 0,
//        entityUuid = association.entityUuid,
//      ),
//    )
//
//    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/counter-sign")
//      .header(HttpHeaders.CONTENT_TYPE, "application/json")
//      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
//      .bodyValue(
//        OasysCounterSignRequest(
//          sanVersionNumber = countersignedVersion.version,
//          sentencePlanVersionNumber = 0,
//          outcome = CounterSignOutcome.COUNTERSIGNED,
//          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
//        ),
//      )
//      .accept(MediaType.APPLICATION_JSON)
//      .exchange()
//      .expectStatus().isEqualTo(409)
//      .expectBody(ErrorResponse::class.java)
//      .returnResult().responseBody
//
//    assertThat(response?.userMessage).isEqualTo("Failed to countersign ASSESSMENT entity due to a conflict")
//  }

  @Test
  fun `it successfully rejects an existing sentence plan without SAN`() {
    val planAssociation = oasysAssociationRepository.save(
      OasysAssociation(
        oasysAssessmentPk = getRandomOasysPk(),
        entityType = EntityType.AAP_PLAN,
        entityUuid = UUID.randomUUID(),
      ),
    )

    val planVersion = oasysVersionRepository.save(
      OasysVersionEntity(
        createdBy = OasysEvent.AWAITING_COUNTERSIGN,
        entityUuid = planAssociation.entityUuid,
        version = 0,
      ),
    )

    val request = OasysCounterSignRequest(
      sanVersionNumber = 0,
      sentencePlanVersionNumber = planVersion.version,
      outcome = CounterSignOutcome.REJECTED,
      userDetails = OasysUserDetails(id = "1", name = "Test Name"),
    )

    val response = webTestClient.post().uri("/oasys/${planAssociation.oasysAssessmentPk}/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody(OasysVersionedEntityResponse::class.java)
      .returnResult()
      .responseBody

    val updatedPlanVersion =
      oasysVersionRepository.findByEntityUuidAndVersion(planAssociation.entityUuid, planVersion.version)

    assertThat(response?.sanAssessmentId).isEqualTo(UUID(0, 0))
    assertThat(response?.sentencePlanId).isEqualTo(planVersion.entityUuid)
    assertThat(response?.sentencePlanVersion).isEqualTo(0)
    assertThat(updatedPlanVersion?.createdBy).isEqualTo(OasysEvent.REJECTED)
  }

  @Test
  fun `it returns a 404 when no associations found`() {
    val request = OasysCounterSignRequest(
      sanVersionNumber = 0,
      sentencePlanVersionNumber = 0,
      outcome = CounterSignOutcome.COUNTERSIGNED,
      userDetails = OasysUserDetails(id = "1", name = "Test Name"),
    )

    webTestClient.post().uri("/oasys/999/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `it returns 400 when validation errors occur in both the path parameter and body`() {
    val sixteenCharPk = "0123456789012345A"
    val thirtyOneCharId = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345"
    val longName = "SomebodyHasAReallyLongFirstName ItsAlmostAsLongAsTheirSurnameButNotQuite"

    val request = OasysCounterSignRequest(
      sanVersionNumber = -1,
      sentencePlanVersionNumber = -1,
      outcome = CounterSignOutcome.COUNTERSIGNED,
      userDetails = OasysUserDetails(id = thirtyOneCharId, name = longName),
    )

    val response = webTestClient.post().uri("/oasys/$sixteenCharPk/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - size must be between 1 and 15")
    assertThat(response.responseBody?.developerMessage).contains("sanVersionNumber - must be greater than or equal to 0")
    assertThat(response.responseBody?.developerMessage).contains("sentencePlanVersionNumber - must be greater than or equal to 0")
    assertThat(response.responseBody?.developerMessage).contains("userDetails.name - size must be between 0 and ${Constraints.OASYS_USER_NAME_MAX_LENGTH}")
    assertThat(response.responseBody?.developerMessage).contains("userDetails.id - size must be between 0 and ${Constraints.OASYS_USER_ID_MAX_LENGTH}")
  }

  @Test
  fun `it returns 400 when validation errors occur in the body only`() {
    val thirtyOneCharId = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345"
    val longName = "SomebodyHasAReallyLongFirstName ItsAlmostAsLongAsTheirSurnameButNotQuite"

    val request = OasysCounterSignRequest(
      sanVersionNumber = -1,
      sentencePlanVersionNumber = -1,
      outcome = CounterSignOutcome.COUNTERSIGNED,
      userDetails = OasysUserDetails(id = thirtyOneCharId, name = longName),
    )

    val response = webTestClient.post().uri("/oasys/012345678901234/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("userDetails.id - size must be between 0 and ${Constraints.OASYS_USER_ID_MAX_LENGTH}")
    assertThat(response.responseBody?.developerMessage).contains("userDetails.name - size must be between 0 and ${Constraints.OASYS_USER_NAME_MAX_LENGTH}")
    assertThat(response.responseBody?.developerMessage).contains("sentencePlanVersionNumber - must be greater than or equal to 0")
    assertThat(response.responseBody?.developerMessage).contains("sanVersionNumber - must be greater than or equal to 0")
  }

  @Test
  fun `it returns 400 when validation errors occur in the path parameter only`() {
    val sixteenCharPk = "0123456789012345"

    val request = OasysCounterSignRequest(
      sanVersionNumber = 1,
      sentencePlanVersionNumber = 1,
      outcome = CounterSignOutcome.COUNTERSIGNED,
      userDetails = OasysUserDetails(id = "1", name = "Test Name"),
    )

    val response = webTestClient.post().uri("/oasys/$sixteenCharPk/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).isEqualTo("[oasysAssessmentPK - size must be between 1 and 15]")
  }
}

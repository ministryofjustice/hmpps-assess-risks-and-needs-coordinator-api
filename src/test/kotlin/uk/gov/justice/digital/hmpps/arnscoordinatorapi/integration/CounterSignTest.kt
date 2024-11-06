package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.*

class CounterSignTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsCounterSign()
    stubSentencePlanCounterSign()
  }

  @Test
  fun `it successfully countersigns an existing SP and SAN for an oasys PK`() {
    val oasysAssessmentPk = "199"
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

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCounterSignRequest(
          sanVersionNumber = 0,
          sentencePlanVersionNumber = 0,
          outcome = CounterSignOutcome.COUNTERSIGNED,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody(OasysGetResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.sanAssessmentId).isEqualTo(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
    assertThat(response?.sanAssessmentVersion).isEqualTo(1)
  }

  @Test
  fun `it returns a 409 when the SAN assessment is already locked`() {
    stubAssessmentsCounterSign(409)
    val oasysAssessmentPk = "200"
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          id = 1L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
      ),
    )

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCounterSignRequest(
          sanVersionNumber = 0,
          sentencePlanVersionNumber = 0,
          outcome = CounterSignOutcome.COUNTERSIGNED,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("Failed to countersign ASSESSMENT entity due to a conflict")
  }

  @Test
  fun `it returns a 409 when the Sentence Plan is already locked`() {
    stubSentencePlanCounterSign(409)
    val oasysAssessmentPk = "201"
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          id = 1L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
      ),
    )

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCounterSignRequest(
          sanVersionNumber = 0,
          sentencePlanVersionNumber = 0,
          outcome = CounterSignOutcome.COUNTERSIGNED,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("Failed to countersign PLAN entity due to a conflict")
  }

  @Test
  fun `it returns a 404 when no associations found`() {
    webTestClient.post().uri("/oasys/999/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCounterSignRequest(
          sanVersionNumber = 0,
          sentencePlanVersionNumber = 0,
          outcome = CounterSignOutcome.COUNTERSIGNED,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `it returns 400 when validation errors occur in both the path parameter and body`() {
    val sixteenCharPk = "0123456789012345A"
    val sixteenCharId = "ABCDEFGHIJKLMNOP"
    val longName = "SomebodyHasAReallyLongFirstName ItsAlmostAsLongAsTheirSurnameButNotQuite"

    val response = webTestClient.post().uri("/oasys/$sixteenCharPk/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCounterSignRequest(
          sanVersionNumber = -1,
          sentencePlanVersionNumber = -1,
          outcome = CounterSignOutcome.COUNTERSIGNED,
          userDetails = OasysUserDetails(id = sixteenCharId, name = longName),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - size must be between 1 and 15")
    assertThat(response.responseBody?.developerMessage).contains("sanVersionNumber - must be greater than or equal to 0")
    assertThat(response.responseBody?.developerMessage).contains("sentencePlanVersionNumber - must be greater than or equal to 0")
    assertThat(response.responseBody?.developerMessage).contains("userDetails.name - size must be between 0 and 64")
    assertThat(response.responseBody?.developerMessage).contains("userDetails.id - size must be between 0 and 15")
  }

  @Test
  fun `it returns 400 when validation errors occur in the body only`() {
    val sixteenCharId = "ABCDEFGHIJKLMNOP"
    val longName = "SomebodyHasAReallyLongFirstName ItsAlmostAsLongAsTheirSurnameButNotQuite"

    val response = webTestClient.post().uri("/oasys/012345678901234/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCounterSignRequest(
          sanVersionNumber = -1,
          sentencePlanVersionNumber = -1,
          outcome = CounterSignOutcome.COUNTERSIGNED,
          userDetails = OasysUserDetails(id = sixteenCharId, name = longName),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("userDetails.id - size must be between 0 and 15")
    assertThat(response.responseBody?.developerMessage).contains("userDetails.name - size must be between 0 and 64")
    assertThat(response.responseBody?.developerMessage).contains("sentencePlanVersionNumber - must be greater than or equal to 0")
    assertThat(response.responseBody?.developerMessage).contains("sanVersionNumber - must be greater than or equal to 0")
  }

  @Test
  fun `it returns 400 when validation errors occur in the path parameter only`() {
    val sixteenCharPk = "0123456789012345"

    val response = webTestClient.post().uri("/oasys/$sixteenCharPk/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCounterSignRequest(
          sanVersionNumber = 1,
          sentencePlanVersionNumber = 1,
          outcome = CounterSignOutcome.COUNTERSIGNED,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).isEqualTo("[oasysAssessmentPK - size must be between 1 and 15]")
  }

  @Test
  fun `it returns 400 when the CounterSignOutcome is not one of the enum values`() {
    val request = """
        {
          "sanVersionNumber":1,
          "sentencePlanVersionNumber":1,
          "outcome":"OUTCOME",
          "userDetails":
          {
            "id":"1",
            "name":"Test Name"
          }
        }
    """.trimIndent()

    val response = webTestClient.post().uri("/oasys/999/counter-sign")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation())
      .bodyValue(request)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.userMessage).startsWith("Validation failure: JSON parse error")
    assertThat(response.responseBody?.developerMessage).startsWith("JSON parse error: Cannot deserialize value of type")
  }
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysMergeRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysMessageResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysTransferAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@ExtendWith(OutputCaptureExtension::class)
class MergeTest : IntegrationTestBase() {
  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
  }

  @Test
  fun `it successfully merges two pairs of OASys PKs`() {
    val existingOasysPk1 = getRandomOasysPk()
    val existingOasysPk2 = getRandomOasysPk()

    val newOasysPk1 = getRandomOasysPk()
    val newOasysPk2 = getRandomOasysPk()

    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(oasysAssessmentPk = existingOasysPk1, entityType = EntityType.PLAN),
        OasysAssociation(oasysAssessmentPk = existingOasysPk1, entityType = EntityType.ASSESSMENT),
        OasysAssociation(oasysAssessmentPk = existingOasysPk2, entityType = EntityType.PLAN),
        OasysAssociation(oasysAssessmentPk = existingOasysPk2, entityType = EntityType.ASSESSMENT),
      ),
    )

    val response = webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(oldOasysAssessmentPK = existingOasysPk1, newOasysAssessmentPK = newOasysPk1),
            OasysTransferAssociation(oldOasysAssessmentPK = existingOasysPk2, newOasysAssessmentPK = newOasysPk2),
          ),
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody(OasysMessageResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.message).isEqualTo("Successfully processed all 2 merge elements")

    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(existingOasysPk1)).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(existingOasysPk2)).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(newOasysPk1).size).isEqualTo(2)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(newOasysPk2).size).isEqualTo(2)
  }

  @Test
  fun `it successfully merges a pair of OASys PKs when the existing PK is soft deleted`() {
    val existingOasysPk = getRandomOasysPk()

    val newOasysPk = getRandomOasysPk()

    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(oasysAssessmentPk = existingOasysPk, entityType = EntityType.PLAN, deleted = true),
        OasysAssociation(oasysAssessmentPk = existingOasysPk, entityType = EntityType.ASSESSMENT, deleted = true),
      ),
    )

    val response = webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(oldOasysAssessmentPK = existingOasysPk, newOasysAssessmentPK = newOasysPk),
          ),
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody(OasysMessageResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.message).isEqualTo("Successfully processed all 1 merge elements")

    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPkIncludingDeleted(existingOasysPk)).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPkIncludingDeleted(newOasysPk).size).isEqualTo(2)
  }

  @Test
  fun `it returns a 409 when an association already exists for the new OASys PK`() {
    val existingAssociationPk1 = getRandomOasysPk()
    val existingAssociationPk2 = getRandomOasysPk()
    val existingAssociationPk3 = getRandomOasysPk()
    val existingAssociationPk4 = getRandomOasysPk()

    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(oasysAssessmentPk = existingAssociationPk1, entityType = EntityType.PLAN),
        OasysAssociation(oasysAssessmentPk = existingAssociationPk2, entityType = EntityType.PLAN),
        OasysAssociation(oasysAssessmentPk = existingAssociationPk3, entityType = EntityType.PLAN),
        OasysAssociation(oasysAssessmentPk = existingAssociationPk4, entityType = EntityType.PLAN),
      ),
    )

    val response = webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(
              oldOasysAssessmentPK = existingAssociationPk1,
              newOasysAssessmentPK = existingAssociationPk2,
            ),
            OasysTransferAssociation(
              oldOasysAssessmentPK = existingAssociationPk3,
              newOasysAssessmentPK = existingAssociationPk4,
            ),
          ),
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("Existing association(s) for $existingAssociationPk2, $existingAssociationPk4")

    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(existingAssociationPk1).size).isEqualTo(1)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(existingAssociationPk2).size).isEqualTo(1)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(existingAssociationPk3).size).isEqualTo(1)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(existingAssociationPk4).size).isEqualTo(1)
  }

  @Test
  fun `it returns a 404 when an association is not found`() {
    val oasysAssessmentPk = getRandomOasysPk()
    val newOasysAssessmentPk = getRandomOasysPk()

    val missingOasysAssessmentPk1 = getRandomOasysPk()
    val newMissingOasysAssessmentPk1 = getRandomOasysPk()

    val missingOasysAssessmentPk2 = getRandomOasysPk()
    val newMissingOasysAssessmentPk2 = getRandomOasysPk()

    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(oasysAssessmentPk = oasysAssessmentPk, entityType = EntityType.PLAN),
      ),
    )

    val response = webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(
              oldOasysAssessmentPK = oasysAssessmentPk,
              newOasysAssessmentPK = newOasysAssessmentPk,
            ),
            OasysTransferAssociation(
              oldOasysAssessmentPK = missingOasysAssessmentPk1,
              newOasysAssessmentPK = newMissingOasysAssessmentPk1,
            ),
            OasysTransferAssociation(
              oldOasysAssessmentPK = missingOasysAssessmentPk2,
              newOasysAssessmentPK = newMissingOasysAssessmentPk2,
            ),
          ),
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("The following association(s) could not be located: $missingOasysAssessmentPk1, $missingOasysAssessmentPk2 and the operation has not been actioned.")

    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk).size).isEqualTo(1)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(newOasysAssessmentPk)).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(missingOasysAssessmentPk1)).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(newMissingOasysAssessmentPk1)).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(missingOasysAssessmentPk2)).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(newMissingOasysAssessmentPk2)).isEmpty()
  }

  @Test
  fun `it returns 400 and validation errors when invalid PKs submitted`() {
    val invalidOasysAssessmentPk = "012345678901234A"

    val response = webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(invalidOasysAssessmentPk, invalidOasysAssessmentPk),
            OasysTransferAssociation(invalidOasysAssessmentPk, invalidOasysAssessmentPk),
          ),
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("oasysMergeRequest.merge[0].oldOasysAssessmentPK - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysMergeRequest.merge[0].oldOasysAssessmentPK - size must be between 1 and 15")
    assertThat(response.responseBody?.developerMessage).contains("oasysMergeRequest.merge[0].newOasysAssessmentPK - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysMergeRequest.merge[0].newOasysAssessmentPK - size must be between 1 and 15")
    assertThat(response.responseBody?.developerMessage).contains("oasysMergeRequest.merge[1].oldOasysAssessmentPK - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysMergeRequest.merge[1].oldOasysAssessmentPK - size must be between 1 and 15")
    assertThat(response.responseBody?.developerMessage).contains("oasysMergeRequest.merge[1].newOasysAssessmentPK - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysMergeRequest.merge[1].newOasysAssessmentPK - size must be between 1 and 15")
  }

  @Test
  fun `it returns 400 and a validation error when an empty merge list is submitted`() {
    val response = webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(),
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).isEqualTo("[oasysMergeRequest.merge - must not be empty]")
  }

  @Test
  fun `log message does not contain crlf characters`(output: CapturedOutput) {
    val existingOasysPk1 = getRandomOasysPk()
    val existingOasysPk2 = getRandomOasysPk()

    val newOasysPk1 = getRandomOasysPk()
    val newOasysPk2 = getRandomOasysPk()

    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(oasysAssessmentPk = existingOasysPk1, entityType = EntityType.PLAN),
        OasysAssociation(oasysAssessmentPk = existingOasysPk1, entityType = EntityType.ASSESSMENT),
        OasysAssociation(oasysAssessmentPk = existingOasysPk2, entityType = EntityType.PLAN),
        OasysAssociation(oasysAssessmentPk = existingOasysPk2, entityType = EntityType.ASSESSMENT),
      ),
    )

    webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(oldOasysAssessmentPK = existingOasysPk1, newOasysAssessmentPK = newOasysPk1),
            OasysTransferAssociation(oldOasysAssessmentPK = existingOasysPk2, newOasysAssessmentPK = newOasysPk2),
          ),
          userDetails = OasysUserDetails(id = "\r\nForged the log\r\n", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isOk

    assertThat(output.out).contains("Forged the log : From $existingOasysPk1 to $newOasysPk1, From $existingOasysPk2 to $newOasysPk2")
  }
}

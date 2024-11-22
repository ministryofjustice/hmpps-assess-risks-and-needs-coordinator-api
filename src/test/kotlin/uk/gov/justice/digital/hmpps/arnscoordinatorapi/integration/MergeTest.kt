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
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(id = 1L, oasysAssessmentPk = "101", entityType = EntityType.PLAN),
        OasysAssociation(id = 2L, oasysAssessmentPk = "101", entityType = EntityType.ASSESSMENT),
        OasysAssociation(id = 3L, oasysAssessmentPk = "102", entityType = EntityType.PLAN),
        OasysAssociation(id = 4L, oasysAssessmentPk = "102", entityType = EntityType.ASSESSMENT),
      ),
    )

    val response = webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(oldOasysAssessmentPK = "101", newOasysAssessmentPK = "201"),
            OasysTransferAssociation(oldOasysAssessmentPK = "102", newOasysAssessmentPK = "202"),
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

    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("101")).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("102")).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("201").size).isEqualTo(2)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("202").size).isEqualTo(2)
  }

  @Test
  fun `it returns a 409 when an association already exists for the new OASys PK`() {
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(id = 1L, oasysAssessmentPk = "103", entityType = EntityType.PLAN),
        OasysAssociation(id = 2L, oasysAssessmentPk = "203", entityType = EntityType.PLAN),
        OasysAssociation(id = 3L, oasysAssessmentPk = "104", entityType = EntityType.PLAN),
        OasysAssociation(id = 4L, oasysAssessmentPk = "204", entityType = EntityType.PLAN),
      ),
    )

    val response = webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(oldOasysAssessmentPK = "103", newOasysAssessmentPK = "203"),
            OasysTransferAssociation(oldOasysAssessmentPK = "104", newOasysAssessmentPK = "204"),
          ),
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("Existing association(s) for 203, 204")

    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("103").size).isEqualTo(1)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("203").size).isEqualTo(1)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("104").size).isEqualTo(1)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("204").size).isEqualTo(1)
  }

  @Test
  fun `it returns a 404 when an association is not found`() {
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(id = 1L, oasysAssessmentPk = "105", entityType = EntityType.PLAN),
      ),
    )

    val response = webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(oldOasysAssessmentPK = "105", newOasysAssessmentPK = "205"),
            OasysTransferAssociation(oldOasysAssessmentPK = "1010", newOasysAssessmentPK = "2010"),
            OasysTransferAssociation(oldOasysAssessmentPK = "1012", newOasysAssessmentPK = "2012"),
          ),
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("The following association(s) could not be located: 1010, 1012 and the operation has not been actioned.")

    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("105").size).isEqualTo(1)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("205")).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("1010")).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("2010")).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("1012")).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("2012")).isEmpty()
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
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(id = 1L, oasysAssessmentPk = "101", entityType = EntityType.PLAN),
        OasysAssociation(id = 2L, oasysAssessmentPk = "101", entityType = EntityType.ASSESSMENT),
        OasysAssociation(id = 3L, oasysAssessmentPk = "102", entityType = EntityType.PLAN),
        OasysAssociation(id = 4L, oasysAssessmentPk = "102", entityType = EntityType.ASSESSMENT),
      ),
    )

    webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(oldOasysAssessmentPK = "101", newOasysAssessmentPK = "201"),
            OasysTransferAssociation(oldOasysAssessmentPK = "102", newOasysAssessmentPK = "202"),
          ),
          userDetails = OasysUserDetails(id = "\r\nForged the log\r\n", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isOk

    assertThat(output.out).contains("Forged the log : From 101 to 201, From 102 to 202")
  }
}

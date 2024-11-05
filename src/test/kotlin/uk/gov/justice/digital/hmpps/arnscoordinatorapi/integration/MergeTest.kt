package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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
        OasysAssociation(id = 1L, oasysAssessmentPk = "OLD-1", entityType = EntityType.PLAN),
        OasysAssociation(id = 2L, oasysAssessmentPk = "OLD-1", entityType = EntityType.ASSESSMENT),
        OasysAssociation(id = 3L, oasysAssessmentPk = "OLD-2", entityType = EntityType.PLAN),
        OasysAssociation(id = 4L, oasysAssessmentPk = "OLD-2", entityType = EntityType.ASSESSMENT),
      ),
    )

    val response = webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(oldOasysAssessmentPK = "OLD-1", newOasysAssessmentPK = "NEW-1"),
            OasysTransferAssociation(oldOasysAssessmentPK = "OLD-2", newOasysAssessmentPK = "NEW-2"),
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

    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("OLD-1")).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("OLD-2")).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("NEW-1").size).isEqualTo(2)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("NEW-2").size).isEqualTo(2)
  }

  @Test
  fun `it returns a 409 when an association already exists for the new OASys PK`() {
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(id = 1L, oasysAssessmentPk = "OLD-3", entityType = EntityType.PLAN),
        OasysAssociation(id = 2L, oasysAssessmentPk = "NEW-3", entityType = EntityType.PLAN),
        OasysAssociation(id = 3L, oasysAssessmentPk = "OLD-4", entityType = EntityType.PLAN),
        OasysAssociation(id = 4L, oasysAssessmentPk = "NEW-4", entityType = EntityType.PLAN),
      ),
    )

    val response = webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(oldOasysAssessmentPK = "OLD-3", newOasysAssessmentPK = "NEW-3"),
            OasysTransferAssociation(oldOasysAssessmentPK = "OLD-4", newOasysAssessmentPK = "NEW-4"),
          ),
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("Existing association(s) for NEW-3, NEW-4")

    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("OLD-3").size).isEqualTo(1)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("NEW-3").size).isEqualTo(1)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("OLD-4").size).isEqualTo(1)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("NEW-4").size).isEqualTo(1)
  }

  @Test
  fun `it returns a 404 when an association is not found`() {
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(id = 1L, oasysAssessmentPk = "OLD-5", entityType = EntityType.PLAN),
      ),
    )

    val response = webTestClient.post().uri("/oasys/merge")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysMergeRequest(
          merge = listOf(
            OasysTransferAssociation(oldOasysAssessmentPK = "OLD-5", newOasysAssessmentPK = "NEW-5"),
            OasysTransferAssociation(oldOasysAssessmentPK = "OLD-10", newOasysAssessmentPK = "NEW-10"),
            OasysTransferAssociation(oldOasysAssessmentPK = "OLD-12", newOasysAssessmentPK = "NEW-12"),
          ),
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).isEqualTo("The following association(s) could not be located: OLD-10, OLD-12 and the operation has not been actioned.")

    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("OLD-5").size).isEqualTo(1)
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("NEW-5")).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("OLD-10")).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("NEW-10")).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("OLD-12")).isEmpty()
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk("NEW-12")).isEmpty()
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
}

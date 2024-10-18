package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.util.*

class SoftDeleteTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsSoftDelete()
    stubSentencePlanSoftDelete()
  }

  @Test
  fun `it successfully soft-deletes an existing association`() {
    val oasysAssessmentPk = "199"
    val assessmentUuid = UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa6")
    val planUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")

    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          id = 1L,
          createdAt = LocalDateTime.now().minusDays(1),
          oasysAssessmentPk = "198",
          entityType = EntityType.PLAN,
          entityUuid = planUuid,
          baseVersion = 1,
        ),
        OasysAssociation(
          id = 2L,
          createdAt = LocalDateTime.now().minusDays(1),
          oasysAssessmentPk = "198",
          entityType = EntityType.ASSESSMENT,
          entityUuid = assessmentUuid,
          baseVersion = 2,
        ),
        OasysAssociation(
          id = 3L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = planUuid,
          baseVersion = 2,
        ),
        OasysAssociation(
          id = 4L,
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = assessmentUuid,
          baseVersion = 3,
        ),
      ),
    )

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/soft-delete")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysGenericRequest(
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody(OasysVersionedEntityResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.sanAssessmentId).isEqualTo(assessmentUuid)
    assertThat(response?.sanAssessmentVersion).isEqualTo(2)

    assertThat(response?.sentencePlanId).isEqualTo(planUuid)
    assertThat(response?.sentencePlanVersion).isEqualTo(3)

    oasysAssociationRepository.findAllByEntityUuidIncludingDeleted(assessmentUuid).run {
      assertEquals(2, count())
      map {
        assertTrue(
          when (it.baseVersion) {
            2L -> !it.deleted
            3L -> it.deleted
            else -> false
          },
        )
      }
    }

    oasysAssociationRepository.findAllByEntityUuidIncludingDeleted(planUuid).run {
      assertEquals(2, count())
      map {
        assertTrue(
          when (it.baseVersion) {
            1L -> !it.deleted
            2L -> it.deleted
            else -> false
          },
        )
      }
    }
  }

  @Test
  fun `it returns a 409 when the SAN assessment is already soft-deleted`() {
    stubAssessmentsSoftDelete(409)
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

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/soft-delete")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysGenericRequest(
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).startsWith("Failed to soft-delete ASSESSMENT versions from 0 to null due to a conflict, Unable to soft-delete the requested assessment versions")
  }

  @Test
  fun `it returns a 409 when the Sentence Plan version(s) is already soft-deleted`() {
    stubSentencePlanSoftDelete(409)
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

    val response = webTestClient.post().uri("/oasys/$oasysAssessmentPk/soft-delete")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysGenericRequest(
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(409)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(response?.userMessage).startsWith("Failed to soft-delete PLAN versions from 0 to null due to a conflict, Unable to soft-delete the requested sentence plan versions")
  }

  @Test
  fun `it returns a 404 when no associations found`() {
    webTestClient.post().uri("/oasys/999/soft-delete")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysGenericRequest(
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNotFound
  }
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
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
    val oasysAssessmentPk = getRandomOasysPk()
    val unaffectedOasysAssessmentPk = getRandomOasysPk()
    val assessmentUuid = UUID.fromString("61369578-18f5-488c-bc99-7cc6249f39a2")
    val planUuid = UUID.fromString("3fc52df3-ad01-40d5-b29c-eba6573faf91")

    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          createdAt = LocalDateTime.now().minusDays(1),
          oasysAssessmentPk = unaffectedOasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = planUuid,
          baseVersion = 1,
        ),
        OasysAssociation(
          createdAt = LocalDateTime.now().minusDays(1),
          oasysAssessmentPk = unaffectedOasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = assessmentUuid,
          baseVersion = 2,
        ),
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = planUuid,
          baseVersion = 2,
        ),
        OasysAssociation(
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
  fun `it successfully soft-deletes the only existing association`() {
    val oasysAssessmentPk = getRandomOasysPk()
    val assessmentUuid = UUID.randomUUID()
    val planUuid = UUID.randomUUID()

    stubAssessmentsSoftDelete(200, emptyBody = true)
    stubSentencePlanSoftDelete(200, planUuid)

    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = planUuid,
          baseVersion = 0,
        ),
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = assessmentUuid,
          baseVersion = 0,
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

    assertThat(response!!.sanAssessmentId).isNull()
    assertThat(response.sanAssessmentVersion).isNull()
    assertThat(response.sentencePlanId).isEqualTo(planUuid)
    assertThat(response.sentencePlanVersion).isEqualTo(3)

    oasysAssociationRepository.findAllByEntityUuidIncludingDeleted(assessmentUuid).run {
      assertEquals(1, count())
      assertTrue(all { it.deleted })
    }

    oasysAssociationRepository.findAllByEntityUuidIncludingDeleted(planUuid).run {
      assertEquals(1, count())
      assertTrue(all { it.deleted })
    }
  }

  @Test
  fun `it returns a 409 when the SAN assessment is already soft-deleted`() {
    stubAssessmentsSoftDelete(409)
    val oasysAssessmentPk = getRandomOasysPk()
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
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
    val oasysAssessmentPk = getRandomOasysPk()
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
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
    webTestClient.post().uri("/oasys/${getRandomOasysPk()}/soft-delete")
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

  @Test
  fun `it returns 400 when validation errors occur in the path parameter`() {
    val sixteenCharPk = "012345678901234A"

    val response = webTestClient.post().uri("/oasys/$sixteenCharPk/soft-delete")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysGenericRequest(
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - size must be between 1 and 15")
  }
}

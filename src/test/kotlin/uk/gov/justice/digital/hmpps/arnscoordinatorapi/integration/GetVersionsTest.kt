package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.VersionsOnDate
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.VersionsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GetVersionsTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsGetVersions()
    stubSentencePlanGetVersions()
  }

  @Test
  fun `it successfully gets SAN and SP versions for an OASys PK linked to a given Plan or Assessment UUID`() {
    val oasysAssessmentPk = getRandomOasysPk()
    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.PLAN,
          entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("2fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
      ),
    )

    val planResponse = webTestClient.get().uri("/entity/versions/5fa85f64-5717-4562-b3fc-2c963f66afa6")
      .headers(setAuthorisation(roles = listOf("ROLE_SENTENCE_PLAN_READ")))
      .exchange()
      .expectStatus().isEqualTo(200)
      .expectBody(VersionsResponse::class.java)
      .returnResult()
      .responseBody

    val assessmentResponse = webTestClient.get().uri("/entity/versions/2fa85f64-5717-4562-b3fc-2c963f66afa6")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_READ")))
      .exchange()
      .expectStatus().isEqualTo(200)
      .expectBody(VersionsResponse::class.java)
      .returnResult()
      .responseBody

    assertNotNull(assessmentResponse)
    assertThat(planResponse).isEqualTo(assessmentResponse)

    val expectedVersions = sortedMapOf(
      LocalDate.parse("2025-06-23") to VersionsOnDate(
        description = "Assessment and plan updated",
        assessmentVersions = mutableListOf(
          VersionDetails(
            uuid = UUID.fromString("11db45b5-215d-4405-a887-a7efd5216fa2"),
            version = 1,
            createdAt = LocalDateTime.parse("2025-06-23T13:22:54.105"),
            updatedAt = LocalDateTime.parse("2025-06-23T13:22:54.105"),
            status = "LOCKED",
            entityType = EntityType.ASSESSMENT,
          ),
        ),
        planVersions = mutableListOf(
          VersionDetails(
            uuid = UUID.fromString("4da85f64-5717-4562-b3fc-2c963f66afb8"),
            version = 1,
            createdAt = LocalDateTime.parse("2025-06-23T14:44:53.105"),
            updatedAt = LocalDateTime.parse("2025-06-23T14:44:53.105"),
            status = "AWAITING_COUNTERSIGN",
            entityType = EntityType.PLAN,
          ),
        ),
      ),
      LocalDate.parse("2025-05-23") to VersionsOnDate(
        description = "Assessment updated",
        assessmentVersions = mutableListOf(
          VersionDetails(
            uuid = UUID.fromString("61369578-18f5-488c-bc99-7cc6249f39a2"),
            version = 0,
            createdAt = LocalDateTime.parse("2025-05-23T13:22:54.105"),
            updatedAt = LocalDateTime.parse("2025-05-23T13:22:54.105"),
            status = "UNSIGNED",
            entityType = EntityType.ASSESSMENT,
          ),
        ),
        planVersions = mutableListOf(
          VersionDetails(
            uuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
            version = 0,
            createdAt = LocalDateTime.parse("2025-04-23T14:40:53.105"),
            updatedAt = LocalDateTime.parse("2025-04-23T14:40:53.105"),
            status = "UNSIGNED",
            entityType = EntityType.PLAN,
          ),
        ),
      ),
      LocalDate.parse("2025-04-23") to VersionsOnDate(
        description = "Plan updated",
        planVersions = mutableListOf(
          VersionDetails(
            uuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
            version = 0,
            createdAt = LocalDateTime.parse("2025-04-23T14:40:53.105"),
            updatedAt = LocalDateTime.parse("2025-04-23T14:40:53.105"),
            status = "UNSIGNED",
            entityType = EntityType.PLAN,
          ),
        ),
      ),
    )

    assertThat(assessmentResponse.versions).isEqualTo(expectedVersions)
  }

  @Test
  fun `get versions by a non-existent entity UUID returns not found`() {
    webTestClient.get().uri("/entity/versions/5dc85f64-5717-4562-b3fc-2c963f66afa7")
      .headers(setAuthorisation(roles = listOf("ROLE_SENTENCE_PLAN_READ")))
      .exchange()
      .expectStatus().isEqualTo(404)
  }
}
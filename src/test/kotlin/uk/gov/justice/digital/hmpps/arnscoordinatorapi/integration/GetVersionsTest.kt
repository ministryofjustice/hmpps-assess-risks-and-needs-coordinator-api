package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.LastVersionsOnDate
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.VersionsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysVersionRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class GetVersionsTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @Autowired
  lateinit var oasysVersionRepository: OasysVersionRepository

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsGetVersions()
    stubAAPQueryAssessmentVersions()
  }

  @Test
  fun `it successfully gets SAN and SP versions for an OASys PK linked to a given Plan or Assessment UUID`() {
    val oasysAssessmentPk = getRandomOasysPk()
    val planEntityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6")
    val sanEntityUuid = UUID.fromString("2fa85f64-5717-4562-b3fc-2c963f66afa6")

    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.AAP_PLAN,
          entityUuid = planEntityUuid,
        ),
        OasysAssociation(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = sanEntityUuid,
        ),
      ),
    )

    oasysVersionRepository
      .findAllByEntityUuid(planEntityUuid)
      .run(oasysVersionRepository::deleteAll)

    val oasysVersion = OasysVersionEntity(
      createdAt = LocalDateTime.parse("2025-06-23T14:44:53.105"),
      createdBy = OasysEvent.AWAITING_COUNTERSIGN,
      updatedAt = LocalDateTime.parse("2025-06-23T14:44:53.105"),
      version = LocalDateTime.parse("2025-06-23T14:44:53.105").toInstant(ZoneOffset.UTC).toEpochMilli(),
      entityUuid = planEntityUuid,
    ).run(oasysVersionRepository::save)

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
      LocalDate.parse("2025-06-23") to LastVersionsOnDate(
        description = "Assessment and plan updated",
        assessmentVersion = VersionDetails(
          uuid = UUID.fromString("11db45b5-215d-4405-a887-a7efd5216fa2"),
          version = 1,
          createdAt = LocalDateTime.parse("2025-06-23T13:22:54.105"),
          updatedAt = LocalDateTime.parse("2025-06-23T13:22:54.105"),
          status = "LOCKED",
          planAgreementStatus = null,
          entityType = EntityType.ASSESSMENT,
        ),
        planVersion = VersionDetails(
          uuid = oasysVersion.uuid,
          version = oasysVersion.version,
          createdAt = LocalDateTime.parse("2025-06-23T14:44:53.105"),
          updatedAt = LocalDateTime.parse("2025-06-23T14:44:53.105"),
          status = "AWAITING_COUNTERSIGN",
          planAgreementStatus = "",
          entityType = EntityType.AAP_PLAN,
        ),
      ),
      LocalDate.parse("2025-05-24") to LastVersionsOnDate(
        description = "Plan updated",
        assessmentVersion = VersionDetails(
          uuid = UUID.fromString("61369578-18f5-488c-bc99-7cc6249f39a2"),
          version = 0,
          createdAt = LocalDateTime.parse("2025-05-23T13:22:54.105"),
          updatedAt = LocalDateTime.parse("2025-05-23T13:22:54.105"),
          status = "UNSIGNED",
          planAgreementStatus = null,
          entityType = EntityType.ASSESSMENT,
        ),
        planVersion = VersionDetails(
          uuid = UUID.fromString("645951e9-15ed-43a1-ac8b-19e97ae0ddf1"),
          version = LocalDateTime.parse("2025-05-24T14:40:53.105").toInstant(ZoneOffset.UTC).toEpochMilli(),
          createdAt = LocalDateTime.parse("2025-05-24T14:40:53.105"),
          updatedAt = LocalDateTime.parse("2025-05-24T14:40:53.105"),
          status = "UNSIGNED",
          planAgreementStatus = "",
          entityType = EntityType.AAP_PLAN,
        ),
      ),
      LocalDate.parse("2025-05-23") to LastVersionsOnDate(
        description = "Assessment updated",
        assessmentVersion = VersionDetails(
          uuid = UUID.fromString("61369578-18f5-488c-bc99-7cc6249f39a2"),
          version = 0,
          createdAt = LocalDateTime.parse("2025-05-23T13:22:54.105"),
          updatedAt = LocalDateTime.parse("2025-05-23T13:22:54.105"),
          status = "UNSIGNED",
          planAgreementStatus = null,
          entityType = EntityType.ASSESSMENT,
        ),
        planVersion = VersionDetails(
          uuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
          version = LocalDateTime.parse("2025-04-23T14:40:53.105").toInstant(ZoneOffset.UTC).toEpochMilli(),
          createdAt = LocalDateTime.parse("2025-04-23T14:40:53.105"),
          updatedAt = LocalDateTime.parse("2025-04-23T14:40:53.105"),
          status = "UNSIGNED",
          planAgreementStatus = "AGREED",
          entityType = EntityType.AAP_PLAN,
        ),
      ),
      LocalDate.parse("2025-04-23") to LastVersionsOnDate(
        description = "Plan updated",
        planVersion = VersionDetails(
          uuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
          version = LocalDateTime.parse("2025-04-23T14:40:53.105").toInstant(ZoneOffset.UTC).toEpochMilli(),
          createdAt = LocalDateTime.parse("2025-04-23T14:40:53.105"),
          updatedAt = LocalDateTime.parse("2025-04-23T14:40:53.105"),
          status = "UNSIGNED",
          planAgreementStatus = "AGREED",
          entityType = EntityType.AAP_PLAN,
        ),
      ),
    )

    assertThat(assessmentResponse.allVersions).isEqualTo(expectedVersions)
    assertThat(assessmentResponse.countersignedVersions).isEmpty()
  }

  @Test
  fun `get versions by a non-existent entity UUID returns not found`() {
    webTestClient.get().uri("/entity/versions/5dc85f64-5717-4562-b3fc-2c963f66afa7")
      .headers(setAuthorisation(roles = listOf("ROLE_SENTENCE_PLAN_READ")))
      .exchange()
      .expectStatus().isEqualTo(404)
  }
}

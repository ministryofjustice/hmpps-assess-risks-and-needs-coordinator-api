package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import java.util.UUID

class EntityAssociationsTest : IntegrationTestBase() {

  @Autowired
  private lateinit var oasysAssociationRepository: OasysAssociationRepository

  @BeforeEach
  fun setUp() {
    oasysAssociationRepository.deleteAll()
  }

  @Nested
  inner class Security {
    @Test
    fun `returns 401 when no auth token provided`() {
      webTestClient.post()
        .uri("/entity/associations")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(listOf(UUID.randomUUID()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `returns 403 when user lacks required role`() {
      webTestClient.post()
        .uri("/entity/associations")
        .headers(setAuthorisation(roles = listOf("ROLE_OTHER")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(listOf(UUID.randomUUID()))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  inner class BulkAssociationLookup {
    @Test
    fun `returns empty map for empty request body`() {
      webTestClient.post()
        .uri("/entity/associations")
        .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(emptyList<UUID>())
        .exchange()
        .expectStatus().isOk
        .expectBody().json("{}")
    }

    @Test
    fun `returns empty map when no associations exist`() {
      webTestClient.post()
        .uri("/entity/associations")
        .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(listOf(UUID.randomUUID()))
        .exchange()
        .expectStatus().isOk
        .expectBody().json("{}")
    }

    @Test
    fun `returns latest association details per entity UUID`() {
      val entityUuid1 = UUID.randomUUID()
      val entityUuid2 = UUID.randomUUID()
      val entityUuid3 = UUID.randomUUID()

      // entityUuid1 has two associations; the second (PK 101) is the latest
      oasysAssociationRepository.saveAll(
        listOf(
          OasysAssociation(entityUuid = entityUuid1, entityType = EntityType.AAP_PLAN, oasysAssessmentPk = "100", regionPrisonCode = "LDN", baseVersion = 1),
          OasysAssociation(entityUuid = entityUuid2, entityType = EntityType.AAP_PLAN, oasysAssessmentPk = "200", regionPrisonCode = "MAN", baseVersion = 5),
        ),
      )
      oasysAssociationRepository.save(
        OasysAssociation(entityUuid = entityUuid1, entityType = EntityType.AAP_PLAN, oasysAssessmentPk = "101", regionPrisonCode = "LDN", baseVersion = 2),
      )

      webTestClient.post()
        .uri("/entity/associations")
        .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(listOf(entityUuid1, entityUuid2, entityUuid3))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          """
          {
            "$entityUuid1": { "oasysAssessmentPk": "101", "regionPrisonCode": "LDN", "baseVersion": 2 },
            "$entityUuid2": { "oasysAssessmentPk": "200", "regionPrisonCode": "MAN", "baseVersion": 5 }
          }
          """,
        )
    }
  }
}

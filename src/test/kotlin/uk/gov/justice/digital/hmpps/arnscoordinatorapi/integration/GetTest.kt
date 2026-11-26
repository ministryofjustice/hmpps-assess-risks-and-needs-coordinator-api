package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.expectBody
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration.wiremock.AAPApiMock.Companion.single
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.plan.PlanState
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.plan.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysAssociationEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.repository.OasysAssociationRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

class GetTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  private val planUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6")
  private val sanUuid = UUID.fromString("4fa85f64-5717-4562-b3fc-2c963f66afa6")

  private val escapedAnswer = "Question answer &amp;, &#x27;, &lt;, &gt;, &#x2F;, &#x5C;, &#96;, &quot;"
  private val unescapedAnswer = "Question answer &, ', <, >, /, \\, `, \""

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    aap.stubQueryAssessment(
      assessmentType = "SENTENCE_PLAN",
      uuid = planUuid,
      properties = mapOf("PLAN_TYPE" to single("REVIEW")),
      collections = listOf(
        mapOf(
          "uuid" to UUID.randomUUID(),
          "createdAt" to "2026-01-09T12:00:00",
          "updatedAt" to "2026-01-09T12:00:00",
          "name" to "PLAN_AGREEMENTS",
          "items" to listOf(
            mapOf(
              "uuid" to UUID.randomUUID(),
              "createdAt" to "2026-01-09T12:00:00",
              "updatedAt" to "2026-01-09T12:00:00",
              "answers" to emptyMap<String, Any>(),
              "properties" to mapOf("status" to single("AGREED")),
              "collections" to emptyList<Any>(),
            ),
          ),
        ),
      ),
    )
    aap.stubQueryAssessment(
      assessmentType = "STRENGTHS_AND_NEEDS",
      uuid = sanUuid,
      answers = mapOf("q2" to single(escapedAnswer)),
      properties = mapOf(
        "accommodation_section_complete" to single("YES"),
        "oasys_equivalent" to single("""{"q2":"$escapedAnswer"}"""),
      ),
    )
  }

  private fun givenAssociations(oasysAssessmentPk: String, vararg types: EntityType) {
    oasysAssociationRepository.saveAll(
      types.map { type ->
        OasysAssociationEntity(
          oasysAssessmentPk = oasysAssessmentPk,
          entityType = type,
          entityUuid = if (type == EntityType.AAP_PLAN) planUuid else sanUuid,
        )
      },
    )
  }

  @Test
  fun `it successfully gets an existing SP and SAN for an oasys PK`() {
    val oasysAssessmentPk = getRandomOasysPk()
    givenAssociations(oasysAssessmentPk, EntityType.AAP_PLAN, EntityType.AAP_SAN)

    val response = webTestClient.get().uri("/oasys/$oasysAssessmentPk")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isEqualTo(200)
      .expectBody(OasysGetResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.sanAssessmentId).isEqualTo(sanUuid)
    assertThat(response?.sanAssessmentVersion).isEqualTo(1767961800000L)
    assertThat(response?.sanAssessmentData).isEqualTo(
      mapOf(
        "q2" to mapOf("value" to unescapedAnswer),
        "accommodation_section_complete" to mapOf("value" to "YES"),
      ),
    )
    assertThat(response?.sanOasysEquivalent).isEqualTo(mapOf("q2" to unescapedAnswer))
    assertThat(response?.sentencePlanId).isEqualTo(planUuid)
    assertThat(response?.sentencePlanVersion).isEqualTo(1767961800000L)
    assertThat(response?.planType).isEqualTo(PlanType.REVIEW)
    assertThat(response?.planComplete).isEqualTo(PlanState.COMPLETE)

    assertThat(aap.queriesSent().map { it.path("assessmentIdentifier").path("uuid").asText() })
      .containsExactly(planUuid.toString(), sanUuid.toString())
  }

  @Test
  fun `it gets the sentence plan when it is the only association`() {
    val oasysAssessmentPk = getRandomOasysPk()
    givenAssociations(oasysAssessmentPk, EntityType.AAP_PLAN)

    val response = webTestClient.get().uri("/oasys/$oasysAssessmentPk")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isOk
      .expectBody(OasysGetResponse::class.java)
      .returnResult()
      .responseBody

    assertThat(response?.sentencePlanId).isEqualTo(planUuid)
    assertThat(response?.sanAssessmentId).isEqualTo(UUID(0, 0))
  }

  @Test
  fun `it escapes non-ASCII characters so the response is pure ASCII`() {
    val text = "£5 café € ‘quoted’ “double” – — … • Gwŷn 😀"
    aap.stubQueryAssessment(
      assessmentType = "STRENGTHS_AND_NEEDS",
      uuid = sanUuid,
      properties = mapOf("oasys_equivalent" to single(JsonMapper().writeValueAsString(mapOf("o1-1" to text)))),
    )
    val oasysAssessmentPk = getRandomOasysPk()
    givenAssociations(oasysAssessmentPk, EntityType.AAP_SAN)

    val body = webTestClient.get().uri("/oasys/$oasysAssessmentPk")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .returnResult()
      .responseBody!!

    assertThat(body.all { it >= 0 }).isTrue()
    assertThat(String(body)).contains("\\u00A3", "\\u2018", "\\uD83D\\uDE00")
    assertThat(JsonMapper().readValue(body, OasysGetResponse::class.java).sanOasysEquivalent).isEqualTo(mapOf("o1-1" to text))
  }

  @Test
  fun `it returns 404 when there are no associations for the oasys PK`() {
    val oasysAssessmentPk = getRandomOasysPk()

    val response = webTestClient.get().uri("/oasys/$oasysAssessmentPk")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isNotFound
      .expectBody<ErrorResponse>()
      .returnResult()
      .responseBody

    assertThat(response?.userMessage).isEqualTo("No associations found for PK $oasysAssessmentPk")
  }

  @Test
  fun `it returns 500 when AAP fails`() {
    aap.stubQueryAssessment(status = 500, assessmentType = "SENTENCE_PLAN", uuid = planUuid)
    val oasysAssessmentPk = getRandomOasysPk()
    givenAssociations(oasysAssessmentPk, EntityType.AAP_PLAN)

    webTestClient.get().uri("/oasys/$oasysAssessmentPk")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().is5xxServerError
  }

  @Test
  fun `it returns 400 when validation errors occur in the path parameter`() {
    val invalidSixteenCharPk = "012345678901234A"

    val response = webTestClient.get().uri("/oasys/$invalidSixteenCharPk")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - size must be between 1 and 15")
    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - Must only contain numeric characters")
  }
}

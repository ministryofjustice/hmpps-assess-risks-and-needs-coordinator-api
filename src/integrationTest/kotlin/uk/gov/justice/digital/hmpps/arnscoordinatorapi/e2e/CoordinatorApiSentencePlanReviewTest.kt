package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.UpdateCommandHelper
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.createOasys.CreateOasysHelper.createOasys
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.entity.GetEntityVersionsHelper.getEntityVersions
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.query.QueryApiHelper.getQueryResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanState
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

@DisplayName("Sentence Plan Review API Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoordinatorApiSentencePlanReviewTest : IntegrationTestBase() {
  private lateinit var sentencePlanId: UUID

  val crn = Random.nextInt(0, 100000).toString().padStart(5, '0')
  val oasysPk = Random.nextInt(0, 1_000_000_000).toString()
  val oasysGenericRequest = OasysGenericRequest(
    userDetails = OasysUserDetails(name = "Test"),
  )
  val today = LocalDate.now()

  @BeforeAll
  fun `create oasys and lock plan`() {
    val createOasysResponse = createOasys(webTestClient, crn, oasysPk)

    assertThat(createOasysResponse?.sentencePlanId).isNotNull

    sentencePlanId = createOasysResponse?.sentencePlanId!!

    UpdateCommandHelper.createGoal(aapTestClient, sentencePlanId)

    val getEntityResponse = webTestClient.get().uri("/entity/$sentencePlanId/AAP_PLAN")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysGetResponse>()
      .returnResult().responseBody

    assertThat(getEntityResponse?.planType).isEqualTo(PlanType.INITIAL)
    assertThat(getEntityResponse?.planComplete).isEqualTo(PlanState.INCOMPLETE)

    webTestClient.post().uri("/oasys/$oasysPk/lock")
      .bodyValue(oasysGenericRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody

    val getEntityVersionsResponse = getEntityVersions(webTestClient, sentencePlanId)
    assertThat(getEntityVersionsResponse?.allVersions?.get(today)?.planVersion?.status).isEqualTo("LOCKED")
  }

  @Test
  fun `review plan`() {
    val oasysReviewPk = Random.nextInt(0, 1_000_000_000).toString()
    // Create a review plan based on previous initial plan
    val createOasysResponse = createOasys(webTestClient, crn, oasysReviewPk, PlanType.REVIEW, previousOasysSpPk = oasysPk)
    val sentencePlanReviewId = createOasysResponse?.sentencePlanId!!

    // Check the plan is cloned on review
    val getEntityVersionsResponse = getEntityVersions(webTestClient, sentencePlanReviewId)
    assertThat(getEntityVersionsResponse?.unaggregatedVersions?.get(0)?.status).isEqualTo("CREATED")
    assertThat(getEntityVersionsResponse?.unaggregatedVersions?.get(1)?.status).isEqualTo("LOCKED")
    assertThat(getEntityVersionsResponse?.unaggregatedVersions?.get(2)?.status).isEqualTo("CLONED")

    // Check that the goals collection is cloned on review
    val queryResponse = getQueryResponse(aapTestClient, sentencePlanReviewId)
    val planVersion = queryResponse?.queries[0]?.result as AssessmentVersionQueryResult
    assertThat(planVersion.collections).isNotEmpty()
    assertThat(planVersion.collections[0].name).isEqualTo("GOALS")
  }
}

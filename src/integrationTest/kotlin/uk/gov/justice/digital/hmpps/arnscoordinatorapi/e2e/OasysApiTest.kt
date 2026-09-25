package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.SubjectDetails


@DisplayName("Coordinator API Tests")
class OasysApiTest : IntegrationTestBase() {

  @Test
  fun `query assessment`() {
    val oasysPk = kotlin.random.Random.nextInt(0, 1_000_000_000).toString()
    val crn = kotlin.random.Random.nextInt(0, 100000).toString().padStart(5, '0')
    val oasysCreateRequest = OasysCreateRequest(
      oasysAssessmentPk = oasysPk,
      subjectDetails = SubjectDetails(crn = crn),
      planType = PlanType.INITIAL,
      assessmentType = AssessmentType.SAN_SP,
      newPeriodOfSupervision = "N",
      regionPrisonCode = "MDI",
      userDetails = OasysUserDetails(id = "test-user", name = "Test User"),
      previousOasysSpPk = null,
      previousOasysSanPk = null
    )

    val createResponse = webTestClient.post().uri("/oasys/create")
      .bodyValue(oasysCreateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isCreated
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody

    assertThat(createResponse?.sanAssessmentVersion).isEqualTo(0)
  }
}

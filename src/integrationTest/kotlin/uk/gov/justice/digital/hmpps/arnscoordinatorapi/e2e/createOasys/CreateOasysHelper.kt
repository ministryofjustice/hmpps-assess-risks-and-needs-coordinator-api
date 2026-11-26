package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.createOasys

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.plan.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.dto.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.dto.SubjectDetails

object CreateOasysHelper {
  fun createOasys(
    webTestClient: WebTestClient,
    crn: String,
    oasysPk: String,
    planType: PlanType = PlanType.INITIAL,
    previousOasysSpPk: String? = null,
    previousOasysSanPk: String? = null,
  ): OasysVersionedEntityResponse? {
    val oasysCreateRequest = OasysCreateRequest(
      oasysAssessmentPk = oasysPk,
      subjectDetails = SubjectDetails(crn = crn),
      planType = planType,
      assessmentType = AssessmentType.SAN_SP,
      newPeriodOfSupervision = "N",
      regionPrisonCode = "MDI",
      userDetails = OasysUserDetails(id = "test-user", name = "Test User"),
      previousOasysSpPk = previousOasysSpPk,
      previousOasysSanPk = previousOasysSanPk,
    )

    val createResponse = webTestClient.post().uri("/oasys/create")
      .bodyValue(oasysCreateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isCreated
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody

    return createResponse
  }
}

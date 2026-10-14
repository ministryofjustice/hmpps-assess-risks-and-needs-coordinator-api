package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.createOasys

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.SubjectDetails

object CreateOasysHelper {
  fun executeCreateOasys(webTestClient: WebTestClient, crn: String, oasysPk: String): OasysVersionedEntityResponse? {
    val oasysCreateRequest = OasysCreateRequest(
      oasysAssessmentPk = oasysPk,
      subjectDetails = SubjectDetails(crn = crn),
      planType = PlanType.INITIAL,
      assessmentType = AssessmentType.SAN_SP,
      newPeriodOfSupervision = "N",
      regionPrisonCode = "MDI",
      userDetails = OasysUserDetails(id = "test-user", name = "Test User"),
      previousOasysSpPk = null,
      previousOasysSanPk = null,
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
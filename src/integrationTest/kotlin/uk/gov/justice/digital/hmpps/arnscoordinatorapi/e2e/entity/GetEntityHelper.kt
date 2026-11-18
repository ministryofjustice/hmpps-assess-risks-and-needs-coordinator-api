package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.entity

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.VersionsResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import java.util.UUID

object GetEntityHelper {
  fun getEntitySentencePlan(webTestClient: WebTestClient, sentencePlanId: UUID): OasysGetResponse? {
    val getResponse = webTestClient.get().uri("/entity/$sentencePlanId/AAP_PLAN")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysGetResponse>()
      .returnResult().responseBody

    return getResponse
  }
}

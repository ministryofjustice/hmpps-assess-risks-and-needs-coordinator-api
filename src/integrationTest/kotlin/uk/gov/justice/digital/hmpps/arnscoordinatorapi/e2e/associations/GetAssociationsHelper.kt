package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.associations

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysAssociationsResponse

object GetAssociationsHelper {
  fun getAssociations(webTestClient: WebTestClient, oasysPk: String): OasysAssociationsResponse? {
    val getResponse = webTestClient.get().uri("/oasys/${oasysPk}/associations")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysAssociationsResponse>()
      .returnResult().responseBody

    return getResponse
  }
}

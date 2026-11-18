package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.lock

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse

object CreateLockHelper {
  fun createLock(webTestClient: WebTestClient, oasysPk: String, oasysGenericRequest: OasysGenericRequest) {
    webTestClient.post().uri("/oasys/$oasysPk/lock")
      .bodyValue(oasysGenericRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody
  }
}

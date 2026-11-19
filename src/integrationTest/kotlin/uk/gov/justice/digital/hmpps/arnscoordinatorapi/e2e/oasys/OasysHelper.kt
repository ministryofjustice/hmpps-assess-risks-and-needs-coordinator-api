package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.oasys

import org.junit.jupiter.api.fail
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse

object OasysHelper {
  fun lock(webTestClient: WebTestClient, oasysPk: String, oasysGenericRequest: OasysGenericRequest) {
    webTestClient.post().uri("/oasys/$oasysPk/lock")
      .bodyValue(oasysGenericRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody
  }

  fun rollback(webTestClient: WebTestClient, oasysPk: String, oasysRollbackRequest: OasysRollbackRequest) {
    val result = webTestClient.post().uri("/oasys/$oasysPk/rollback")
      .bodyValue(oasysRollbackRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()

    when (val status = result.returnResult().status) {
      HttpStatus.OK -> result.expectBody<OasysVersionedEntityResponse>()
      HttpStatus.CONFLICT -> Unit
      else -> fail("Expected HTTP 200 OK or 409 CONFLICT, but got $status")
    }
  }
}


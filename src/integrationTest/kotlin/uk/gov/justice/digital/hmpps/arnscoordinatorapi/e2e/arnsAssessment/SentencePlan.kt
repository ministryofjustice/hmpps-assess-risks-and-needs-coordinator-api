package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.arnsAssessment

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

object SentencePlan {
  fun get(webTestClient: WebTestClient, crn: String): SentencePlanResponse? {
    val queryResponse = webTestClient.get().uri("/sentence-plan/{crn}", crn)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<SentencePlanResponse>()
      .returnResult().responseBody

    return queryResponse
  }
}
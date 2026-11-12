package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.query

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AssessmentIdentifier
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.AssessmentVersionQuery
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.QueriesRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.QueriesResponse
import java.util.UUID

object QueryApiHelper {

  fun getQueryResponse(webTestClient: WebTestClient, identifier: UUID): QueriesResponse? {
    val testUserDetails = AAPUser(id = "test-user", name = "Test User")
    val assessmentVersionQuery = QueriesRequest(
      queries = listOf(
        AssessmentVersionQuery(
          user = testUserDetails,
          assessmentIdentifier = AssessmentIdentifier(identifier),
        ),
      ),
    )

    val queryResponse = webTestClient.post().uri("/query")
      .bodyValue(assessmentVersionQuery)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<QueriesResponse>()
      .returnResult().responseBody

    return queryResponse
  }
}

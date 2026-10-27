package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.entity

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.VersionsResponse
import java.io.File
import java.util.UUID

object GetEntityVersionsHelper {
  fun getEntityVersions(webTestClient: WebTestClient, assessmentUuid: UUID): VersionsResponse? {
    File("build/reports/tests/ci-url.json").writeText(System.getenv("ARNS_COORDINATOR_BASE_URL")+"/entity/versions/$assessmentUuid")
    val getResponse = webTestClient.get().uri("/entity/versions/$assessmentUuid")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<VersionsResponse>()
      .returnResult().responseBody

    return getResponse
  }

  fun getEntityVersionsNotFound(webTestClient: WebTestClient, assessmentUuid: UUID): String? {
    val getResponse = webTestClient.get().uri("/entity/versions/$assessmentUuid")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isNotFound
      .expectBody<String>()
      .returnResult().responseBody

    return getResponse
  }
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class AssociationsTest : IntegrationTestBase() {

  @Test
  fun `it returns 400 when validation errors occur in the path parameter`() {
    val sixteenCharPk = "012345678901234A"

    val response = webTestClient.get().uri("/oasys/$sixteenCharPk/associations")
      .header(HttpHeaders.CONTENT_TYPE, "application/json")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPK - size must be between 1 and 15")
  }
}

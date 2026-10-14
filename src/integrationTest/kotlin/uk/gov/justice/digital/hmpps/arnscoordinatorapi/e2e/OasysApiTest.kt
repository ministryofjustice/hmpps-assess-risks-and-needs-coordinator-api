package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.createOasys.CreateOasysHelper.executeCreateOasys
import kotlin.random.Random

@DisplayName("Oasys API Tests")
class OasysApiTest : IntegrationTestBase() {

  @Test
  fun `create oasys association`() {
    val oasysPk = Random.nextInt(0, 1_000_000_000).toString()
    val crn = Random.nextInt(0, 100000).toString().padStart(5, '0')
    val createOasysResponse = executeCreateOasys(webTestClient, crn, oasysPk)
    assertThat(createOasysResponse?.sanAssessmentVersion).isEqualTo(0)
  }
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.OasysController
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.util.UUID

class OasysControllerTest {

  @Test
  fun `create returns 201 on success`() {
    val service = mock<OasysCoordinatorService>()
    val controller = OasysController(service, JsonMapper())

    val sentencePlanId = UUID.randomUUID()
    val sanAssessmentId = UUID.randomUUID()

    val request = OasysCreateRequest(
      oasysAssessmentPk = "12345",
      regionPrisonCode = "MDI",
      planType = PlanType.INITIAL,
      assessmentType = AssessmentType.SAN_SP,
      userDetails = OasysUserDetails(id = "1", name = "Test Name"),
    )

    whenever(service.create(request)).thenReturn(
      OasysCoordinatorService.CreateOperationResult.Success(
        OasysVersionedEntityResponse(
          sanAssessmentId = sanAssessmentId,
          sanAssessmentVersion = 1,
          sentencePlanId = sentencePlanId,
          sentencePlanVersion = 7,
        ),
      ),
    )

    val response = controller.create(request)
    assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
  }

  @Test
  fun `create returns 500 on failure`() {
    val service = mock<OasysCoordinatorService>()
    val controller = OasysController(service, JsonMapper())

    val request = OasysCreateRequest(
      oasysAssessmentPk = "12345",
      regionPrisonCode = "MDI",
      planType = PlanType.INITIAL,
      assessmentType = AssessmentType.SAN_SP,
      userDetails = OasysUserDetails(id = "1", name = "Test Name"),
    )

    whenever(service.create(request)).thenReturn(
      OasysCoordinatorService.CreateOperationResult.Failure("Failure"),
    )

    val response = controller.create(request)
    assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
  }

  @Test
  fun `get escapes non-ASCII characters so the response is pure ASCII`() {
    val service = mock<OasysCoordinatorService>()
    val controller = OasysController(service, JsonMapper())
    val text = "£5 café € ‘quoted’ “double” – — … • Gwŷn 😀"

    whenever(service.get("12345")).thenReturn(
      OasysCoordinatorService.GetOperationResult.Success(
        OasysGetResponse(sanOasysEquivalent = mapOf("o1-1" to text)),
      ),
    )

    val response = controller.get("12345")
    val body = response.body as ByteArray

    assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
    assertThat(body.all { it >= 0 }).isTrue()
    assertThat(String(body)).contains("\\u00A3", "\\u2018", "\\uD83D\\uDE00")
    assertThat(JsonMapper().readValue(body, OasysGetResponse::class.java).sanOasysEquivalent)
      .isEqualTo(mapOf("o1-1" to text))
  }
}

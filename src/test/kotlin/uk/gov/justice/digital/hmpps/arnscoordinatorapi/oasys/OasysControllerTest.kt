package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.OasysController
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.util.UUID

class OasysControllerTest {

  @Test
  fun `create returns 201 on success`() {
    val service = mock<OasysCoordinatorService>()
    val controller = OasysController(service)

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
    val controller = OasysController(service)

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
}

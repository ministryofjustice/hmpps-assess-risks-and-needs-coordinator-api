package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.CoordinatorEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.EventType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.OasysEvent
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.OasysEventPublisher
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.events.VersionPayload
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.OasysController
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.util.UUID

class OasysControllerTest {
  @Test
  fun `create publishes expected OASYS_VERSION_EVENT to SQS on success`() {
    val service = mock<OasysCoordinatorService>()
    val publisher = mock<OasysEventPublisher>()
    val controller = OasysController(service, publisher)

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

    val captor = argumentCaptor<CoordinatorEvent>()
    verify(publisher).publish(captor.capture())

    val event = captor.firstValue
    assertThat(event.eventType).isEqualTo(EventType.OASYS_VERSION_EVENT)
    assertThat(event.entityType).isEqualTo("AAP_PLAN")
    assertThat(event.entityUuid).isEqualTo(sentencePlanId)

    val payload = event.message as VersionPayload
    assertThat(payload.version).isEqualTo(7)
    assertThat(payload.oasysEvent).isEqualTo(OasysEvent.CREATED)
    assertThat(payload.deleted).isFalse()
    assertThat(payload.association.oasysAssessmentPk).isEqualTo("12345")
    assertThat(payload.association.regionPrisonCode).isEqualTo("MDI")
    assertThat(payload.association.baseVersion).isEqualTo(1L)
  }

  @Test
  fun `create does not publish to SQS when create fails`() {
    val service = mock<OasysCoordinatorService>()
    val publisher = mock<OasysEventPublisher>()
    val controller = OasysController(service, publisher)

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

    controller.create(request)

    verify(publisher, never()).publish(any())
  }
}

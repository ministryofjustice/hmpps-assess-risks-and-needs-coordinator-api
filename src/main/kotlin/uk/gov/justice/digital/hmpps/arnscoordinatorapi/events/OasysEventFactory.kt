package uk.gov.justice.digital.hmpps.arnscoordinatorapi.events

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysMergeRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse

@Component
class OasysEventFactory(
  private val clock: Clock,
) {

  fun createVersionEvent(
    request: OasysCreateRequest,
    result: OasysVersionedEntityResponse,
  ): CoordinatorEvent {
    val now = clock.now()

    return CoordinatorEvent(
      eventType = EventType.OASYS_VERSION_EVENT,
      entityType = "AAP_PLAN",
      entityUuid = result.sentencePlanId,
      occurredAt = now,
      message = VersionPayload(
        version = result.sentencePlanVersion,
        oasysEvent = OasysEvent.CREATED,
        incrementedAt = now,
        deleted = false,
        association = AssociationPayload(
          oasysAssessmentPk = request.oasysAssessmentPk,
          regionPrisonCode = request.regionPrisonCode,
          baseVersion = 1L, // replace with real value
        ),
      ),
    )
  }
}
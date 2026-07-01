package uk.gov.justice.digital.hmpps.arnscoordinatorapi.events

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation

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
          baseVersion = result.sentencePlanVersion,
        ),
      ),
    )
  }

  fun signVersionEvent(
    oasysAssessmentPk: String,
    association: OasysAssociation,
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
        oasysEvent = OasysEvent.SELF_SIGNED,
        incrementedAt = now,
        deleted = false,
        association = AssociationPayload(
          oasysAssessmentPk = oasysAssessmentPk,
          regionPrisonCode = association.regionPrisonCode,
          baseVersion = association.baseVersion,
        ),
      ),
    )
  }

  fun lockVersionEvent(
    oasysAssessmentPk: String,
    association: OasysAssociation,
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
        oasysEvent = OasysEvent.LOCKED,
        incrementedAt = now,
        deleted = false,
        association = AssociationPayload(
          oasysAssessmentPk = oasysAssessmentPk,
          regionPrisonCode = association.regionPrisonCode,
          baseVersion = association.baseVersion,
        ),
      ),
    )
  }

  fun rollbackVersionEvent(
    oasysAssessmentPk: String,
    association: OasysAssociation,
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
        oasysEvent = OasysEvent.ROLLED_BACK,
        incrementedAt = now,
        deleted = false,
        association = AssociationPayload(
          oasysAssessmentPk = oasysAssessmentPk,
          regionPrisonCode = association.regionPrisonCode,
          baseVersion = association.baseVersion,
        ),
      ),
    )
  }

  fun counterSignVersionEvent(
    oasysAssessmentPk: String,
    association: OasysAssociation,
    request: OasysCounterSignRequest,
    result: OasysVersionedEntityResponse,
  ): CoordinatorEvent {
    val now = clock.now()

    val oasysEvent = when (request.outcome) {
      CounterSignOutcome.COUNTERSIGNED -> OasysEvent.COUNTERSIGNED
      CounterSignOutcome.AWAITING_DOUBLE_COUNTERSIGN -> OasysEvent.AWAITING_DOUBLE_COUNTERSIGN
      CounterSignOutcome.DOUBLE_COUNTERSIGNED -> OasysEvent.DOUBLE_COUNTERSIGNED
      CounterSignOutcome.REJECTED -> OasysEvent.REJECTED
    }

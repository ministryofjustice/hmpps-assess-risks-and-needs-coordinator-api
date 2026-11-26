package uk.gov.justice.digital.hmpps.arnscoordinatorapi.events

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.SignType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysAssociationEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysEvent

@Component
class OasysEventFactory(
  private val clock: Clock,
) {

  fun createVersionEvent(request: OasysCreateRequest, result: OasysVersionedEntityResponse): CoordinatorEvent = versionEvent(
    oasysEvent = result.aapPlanVersion?.createdBy ?: OasysEvent.CREATED,
    oasysAssessmentPk = request.oasysAssessmentPk,
    regionPrisonCode = request.regionPrisonCode,
    baseVersion = result.aapPlanVersion?.version ?: 0L,
    result = result,
  )

  fun signVersionEvent(
    association: OasysAssociationEntity,
    request: OasysSignRequest,
    result: OasysVersionedEntityResponse,
  ): CoordinatorEvent = versionEvent(
    oasysEvent = when (request.signType) {
      SignType.SELF -> OasysEvent.SELF_SIGNED
      SignType.COUNTERSIGN -> OasysEvent.AWAITING_COUNTERSIGN
    },
    oasysAssessmentPk = association.oasysAssessmentPk!!,
    regionPrisonCode = association.regionPrisonCode,
    baseVersion = association.baseVersion,
    result = result,
  )

  fun lockVersionEvent(association: OasysAssociationEntity, result: OasysVersionedEntityResponse): CoordinatorEvent = versionEvent(
    oasysEvent = OasysEvent.LOCKED,
    oasysAssessmentPk = association.oasysAssessmentPk!!,
    regionPrisonCode = association.regionPrisonCode,
    baseVersion = association.baseVersion,
    result = result,
  )

  fun rollbackVersionEvent(association: OasysAssociationEntity, result: OasysVersionedEntityResponse): CoordinatorEvent = versionEvent(
    oasysEvent = OasysEvent.ROLLED_BACK,
    oasysAssessmentPk = association.oasysAssessmentPk!!,
    regionPrisonCode = association.regionPrisonCode,
    baseVersion = association.baseVersion,
    result = result,
  )

  fun counterSignVersionEvent(
    association: OasysAssociationEntity,
    request: OasysCounterSignRequest,
    result: OasysVersionedEntityResponse,
  ): CoordinatorEvent {
    val oasysEvent = when (request.outcome) {
      CounterSignOutcome.COUNTERSIGNED -> OasysEvent.COUNTERSIGNED
      CounterSignOutcome.AWAITING_DOUBLE_COUNTERSIGN -> OasysEvent.AWAITING_DOUBLE_COUNTERSIGN
      CounterSignOutcome.DOUBLE_COUNTERSIGNED -> OasysEvent.DOUBLE_COUNTERSIGNED
      CounterSignOutcome.REJECTED -> OasysEvent.REJECTED
    }

    return versionEvent(
      oasysEvent = oasysEvent,
      oasysAssessmentPk = association.oasysAssessmentPk!!,
      regionPrisonCode = association.regionPrisonCode,
      baseVersion = association.baseVersion,
      result = result,
    )
  }

  fun softDeleteEvent(association: OasysAssociationEntity, versionTo: Long?): CoordinatorEvent = deleteFlagEvent(association = association, deleted = true, versionTo = versionTo)

  fun undeleteEvent(association: OasysAssociationEntity, versionTo: Long?): CoordinatorEvent = deleteFlagEvent(association = association, deleted = false, versionTo = versionTo)

  private fun versionEvent(
    oasysEvent: OasysEvent,
    oasysAssessmentPk: String,
    regionPrisonCode: String?,
    baseVersion: Long,
    result: OasysVersionedEntityResponse,
  ): CoordinatorEvent {
    val now = clock.now()
    val aapVersion = result.aapPlanVersion
    return CoordinatorEvent(
      eventType = EventType.OASYS_VERSION_EVENT,
      entityType = "AAP_PLAN",
      entityUuid = result.sentencePlanId,
      occurredAt = now,
      message = VersionPayload(
        version = result.sentencePlanVersion,
        oasysEvent = oasysEvent,
        incrementedAt = aapVersion?.updatedAt ?: now,
        deleted = aapVersion?.deleted ?: false,
        association = AssociationPayload(
          oasysAssessmentPk = oasysAssessmentPk,
          regionPrisonCode = regionPrisonCode,
          baseVersion = baseVersion,
        ),
      ),
    )
  }

  private fun deleteFlagEvent(association: OasysAssociationEntity, deleted: Boolean, versionTo: Long?): CoordinatorEvent = CoordinatorEvent(
    eventType = EventType.OASYS_DELETE_FLAG_UPDATE_EVENT,
    entityType = "AAP_PLAN",
    entityUuid = association.entityUuid,
    occurredAt = clock.now(),
    message = DeleteFlagUpdatePayload(
      deleted = deleted,
      versionFrom = association.baseVersion,
      versionTo = versionTo,
    ),
  )
}

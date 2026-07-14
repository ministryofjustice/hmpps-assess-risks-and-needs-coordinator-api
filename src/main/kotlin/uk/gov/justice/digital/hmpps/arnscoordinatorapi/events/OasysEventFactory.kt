package uk.gov.justice.digital.hmpps.arnscoordinatorapi.events

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent

@Component
class OasysEventFactory(
  private val clock: Clock,
) {

  fun createVersionEvent(request: OasysCreateRequest, result: OasysVersionedEntityResponse): CoordinatorEvent = versionEvent(
    oasysEvent = result.aapPlanVersionedEntity?.createdBy ?: OasysEvent.CREATED,
    oasysAssessmentPk = request.oasysAssessmentPk,
    regionPrisonCode = request.regionPrisonCode,
    baseVersion = result.aapPlanVersionedEntity?.version ?: 0L,
    result = result,
  )

  fun signVersionEvent(
    association: OasysAssociation,
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

  fun lockVersionEvent(association: OasysAssociation, result: OasysVersionedEntityResponse): CoordinatorEvent = versionEvent(
    oasysEvent = OasysEvent.LOCKED,
    oasysAssessmentPk = association.oasysAssessmentPk!!,
    regionPrisonCode = association.regionPrisonCode,
    baseVersion = association.baseVersion,
    result = result,
  )

  fun rollbackVersionEvent(association: OasysAssociation, result: OasysVersionedEntityResponse): CoordinatorEvent = versionEvent(
    oasysEvent = OasysEvent.ROLLED_BACK,
    oasysAssessmentPk = association.oasysAssessmentPk!!,
    regionPrisonCode = association.regionPrisonCode,
    baseVersion = association.baseVersion,
    result = result,
  )

  fun counterSignVersionEvent(
    association: OasysAssociation,
    request: OasysCounterSignRequest,
    result: OasysVersionedEntityResponse,
  ): CoordinatorEvent {
    val oasysEvent = when (request.outcome) {
      // TODO: Why does CounterSignOutcome not have AWAITING_COUNTERSIGN?
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

  fun softDeleteEvent(association: OasysAssociation, versionTo: Long?): CoordinatorEvent = deleteFlagEvent(association = association, deleted = true, versionTo = versionTo)

  fun undeleteEvent(association: OasysAssociation, versionTo: Long?): CoordinatorEvent = deleteFlagEvent(association = association, deleted = false, versionTo = versionTo)

  private fun versionEvent(
    oasysEvent: OasysEvent,
    oasysAssessmentPk: String,
    regionPrisonCode: String?,
    baseVersion: Long,
    result: OasysVersionedEntityResponse,
  ): CoordinatorEvent {
    val now = clock.now()
    val aapVersion = result.aapPlanVersionedEntity
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

  private fun deleteFlagEvent(association: OasysAssociation, deleted: Boolean, versionTo: Long?): CoordinatorEvent = CoordinatorEvent(
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

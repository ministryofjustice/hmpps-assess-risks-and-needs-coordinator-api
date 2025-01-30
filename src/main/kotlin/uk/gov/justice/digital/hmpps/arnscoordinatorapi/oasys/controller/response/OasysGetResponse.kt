package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response

import com.fasterxml.jackson.annotation.JsonInclude
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.AssessmentResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.GetPlanResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanState
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.time.LocalDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OasysGetResponse(
  override var sanAssessmentId: UUID? = null,
  override var sanAssessmentVersion: Long? = null,
  var sanAssessmentData: Map<*, *>? = null,
  var sanOasysEquivalent: Map<String, *>? = null,
  var lastUpdatedTimestampSAN: LocalDateTime? = null,

  override var sentencePlanId: UUID? = null,
  override var sentencePlanVersion: Long? = null,
  var planComplete: PlanState? = null,
  var planType: PlanType? = null,
  var lastUpdatedTimestampSP: LocalDateTime? = null,
) : OasysVersionedEntityResponse(
  sanAssessmentId = sanAssessmentId,
  sanAssessmentVersion = sanAssessmentVersion,
  sentencePlanId = sentencePlanId,
  sentencePlanVersion = sentencePlanVersion,
) {
  fun idFor(entityType: EntityType): UUID? = when (entityType) {
    EntityType.ASSESSMENT -> sanAssessmentId
    EntityType.PLAN -> sentencePlanId
  }

  fun versionFor(entityType: EntityType): Long? = when (entityType) {
    EntityType.ASSESSMENT -> sanAssessmentVersion
    EntityType.PLAN -> sentencePlanVersion
  }

  fun addEntityData(entityData: Any) {
    when (entityData) {
      is GetPlanResponse -> {
        sentencePlanId = entityData.sentencePlanId
        sentencePlanVersion = entityData.sentencePlanVersion
        planType = entityData.planType
        planComplete = entityData.planComplete
        lastUpdatedTimestampSP = entityData.lastUpdatedTimestampSP
      }
      is AssessmentResponse -> {
        sanAssessmentId = entityData.metaData.uuid
        sanAssessmentVersion = entityData.metaData.versionNumber
        sanAssessmentData = entityData.assessment
        sanOasysEquivalent = entityData.oasysEquivalent
        lastUpdatedTimestampSAN = entityData.metaData.versionUpdatedAt
      }
    }
  }
}

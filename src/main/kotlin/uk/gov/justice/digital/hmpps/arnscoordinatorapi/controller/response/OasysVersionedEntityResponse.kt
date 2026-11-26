package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysVersionEntity
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
open class OasysVersionedEntityResponse(
  open var sanAssessmentId: UUID = UUID(0, 0),
  open var sanAssessmentVersion: Long = 0,
  open var sentencePlanId: UUID = UUID(0, 0),
  open var sentencePlanVersion: Long = 0,
) {
  @JsonIgnore
  var aapPlanVersion: OasysVersionEntity? = null
    private set

  open fun populate(oasysVersion: OasysVersionEntity, entityType: EntityType) {
    when (entityType) {
      EntityType.AAP_PLAN -> {
        this.sentencePlanId = oasysVersion.entityUuid
        this.sentencePlanVersion = oasysVersion.version
        this.aapPlanVersion = oasysVersion
      }
      EntityType.AAP_SAN -> {
        this.sanAssessmentId = oasysVersion.entityUuid
        this.sanAssessmentVersion = oasysVersion.version
      }
    }
  }
}

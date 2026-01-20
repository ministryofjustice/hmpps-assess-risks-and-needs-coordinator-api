package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response

import com.fasterxml.jackson.annotation.JsonInclude
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
open class OasysVersionedEntityResponse(
  open var sanAssessmentId: UUID = UUID(0, 0),
  open var sanAssessmentVersion: Long = 0,
  open var sentencePlanId: UUID = UUID(0, 0),
  open var sentencePlanVersion: Long = 0,
) {
  open fun addVersionedEntity(versionedEntity: VersionedEntity) {
    when (versionedEntity.entityType) {
      EntityType.PLAN, EntityType.AAP_PLAN -> {
        this.sentencePlanId = versionedEntity.id
        this.sentencePlanVersion = versionedEntity.version
      }
      EntityType.ASSESSMENT -> {
        this.sanAssessmentId = versionedEntity.id
        this.sanAssessmentVersion = versionedEntity.version
      }
    }
  }
}

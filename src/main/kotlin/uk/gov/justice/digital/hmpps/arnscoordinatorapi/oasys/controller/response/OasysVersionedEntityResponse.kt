package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response

import com.fasterxml.jackson.annotation.JsonInclude
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
open class OasysVersionedEntityResponse(
  open var sanAssessmentId: UUID? = null,
  open var sanAssessmentVersion: Long? = null,
  open var sentencePlanId: UUID? = null,
  open var sentencePlanVersion: Long? = null,
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

  fun applyDefaultAssessmentValues() {
    this.sanAssessmentId = NULL_UUID
    this.sanAssessmentVersion = 0
  }

  companion object {
    val NULL_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
  }
}

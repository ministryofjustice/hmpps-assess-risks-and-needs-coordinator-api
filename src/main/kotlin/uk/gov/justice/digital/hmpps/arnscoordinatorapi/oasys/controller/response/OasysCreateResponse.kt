package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response

import com.fasterxml.jackson.annotation.JsonInclude
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OasysCreateResponse(
  var sanAssessmentId: UUID? = null,
  var sanAssessmentVersion: Long? = null,
  var sentencePlanId: UUID? = null,
  var sentencePlanVersion: Long? = null,
) {
  fun addVersionedEntity(versionedEntity: VersionedEntity) {
    when (versionedEntity.entityType) {
      EntityType.PLAN -> {
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

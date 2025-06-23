package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetailsList
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.time.LocalDate
import java.util.UUID

data class VersionsOnDate(
  var description: String? =  null,
  var assessmentVersionUuid: UUID? = null,
  var planVersionUuid: UUID? = null,
  var planStatus: String? = null,
)

data class VersionsResponse(
  val versions: MutableMap<LocalDate, VersionsOnDate> = mutableMapOf(),
) {
  fun addVersions(versionsToAdd: VersionDetailsList) {
    for (versionToAdd in versionsToAdd) {
      val versionDate = versionToAdd.updatedAt.toLocalDate()
      val versionsOnDate = this.versions[versionDate] ?: VersionsOnDate()
      when (versionToAdd.entityType) {
        EntityType.ASSESSMENT -> {
          versionsOnDate.assessmentVersionUuid = versionToAdd.uuid
        }
        EntityType.PLAN -> {
          versionsOnDate.planVersionUuid = versionToAdd.uuid
          versionsOnDate.planStatus = versionToAdd.status
        }
      }
      this.versions[versionDate] = versionsOnDate
    }
  }
}

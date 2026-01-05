package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetailsList
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.time.LocalDate

class VersionsResponseFactory {
  val versions: MutableMap<LocalDate, VersionsOnDate> = mutableMapOf()

  fun addVersions(versionsToAdd: VersionDetailsList) = versionsToAdd.forEach { versionToAdd ->
    versions.getOrPut(versionToAdd.updatedAt.toLocalDate()) { VersionsOnDate() }
      .let {
        when (versionToAdd.entityType) {
          EntityType.PLAN -> it.planVersions
          EntityType.AAP_PLAN -> it.planVersions
          EntityType.ASSESSMENT -> it.assessmentVersions
        }
      }.add(versionToAdd)
  }

  private fun getVersionsTableForStatuses(statuses: Set<String>): VersionsTable = versions.entries.sortedBy { it.key }
    .fold(mutableMapOf<LocalDate, LastVersionsOnDate>()) { acc, (date, versionsOnDate) ->
      val filteredVersionsOnDate = VersionsOnDate(
        assessmentVersions = versionsOnDate.assessmentVersions.filter { it.status in statuses }.toMutableList(),
        planVersions = versionsOnDate.planVersions.filter { it.status in statuses }.toMutableList(),
      )
      LastVersionsOnDate(
        assessmentVersion = filteredVersionsOnDate.assessmentVersions.maxByOrNull { it.updatedAt },
        planVersion = filteredVersionsOnDate.planVersions.maxByOrNull { it.updatedAt },
        description = getDescription(filteredVersionsOnDate),
      )
        .takeIf { it.planVersion !== null || it.assessmentVersion !== null }
        ?.let { acc.put(date, it) }
      acc
    }
    .toSortedMap(Comparator.reverseOrder())

  private fun getVersionsTable(statusesToExclude: Set<String> = emptySet()): VersionsTable = versions.entries.sortedBy { it.key }
    .fold(object {
      val versionsTable = mutableMapOf<LocalDate, LastVersionsOnDate>()
      var lastAssessment: VersionDetails? = null
      var lastPlan: VersionDetails? = null
    }) { acc, (date, versionsOnDate) ->
      LastVersionsOnDate(
        assessmentVersion = versionsOnDate.assessmentVersions.maxByOrNull { it.updatedAt } ?: acc.lastAssessment,
        planVersion = versionsOnDate.planVersions.maxByOrNull { it.updatedAt } ?: acc.lastPlan,
        description = getDescription(versionsOnDate),
      )
        .also {
          acc.lastAssessment = it.assessmentVersion
          acc.lastPlan = it.planVersion
        }
        .takeUnless {
          it.assessmentVersion?.status?.run(statusesToExclude::contains) ?: true &&
            it.planVersion?.status?.run(statusesToExclude::contains) ?: true
        }
        ?.let {
          acc.versionsTable.put(date, it)
        }
      acc
    }
    .run { versionsTable }
    .toSortedMap(Comparator.reverseOrder())

  private fun getDescription(versionsOnDate: VersionsOnDate): String? = with(versionsOnDate) {
    when {
      assessmentVersions.isNotEmpty() && planVersions.isNotEmpty() -> "Assessment and plan updated"
      assessmentVersions.isNotEmpty() -> "Assessment updated"
      planVersions.isNotEmpty() -> "Plan updated"
      else -> null
    }
  }

  fun getVersionsResponse() = VersionsResponse(
    allVersions = getVersionsTable(setOf("COUNTERSIGNED", "DOUBLE_COUNTERSIGNED")),
    countersignedVersions = getVersionsTableForStatuses(setOf("COUNTERSIGNED", "DOUBLE_COUNTERSIGNED")),
  )
}

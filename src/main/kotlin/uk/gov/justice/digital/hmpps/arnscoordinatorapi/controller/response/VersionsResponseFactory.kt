package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetailsList
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.time.LocalDate

class VersionsResponseFactory {
  val versions: MutableMap<LocalDate, VersionsOnDate> = mutableMapOf()

  fun addVersions(versionsToAdd: VersionDetailsList) = versionsToAdd.forEach { versionToAdd ->
    this.versions.getOrPut(versionToAdd.updatedAt.toLocalDate()) { VersionsOnDate() }
      .let { if (versionToAdd.entityType == EntityType.PLAN) it.planVersions else it.assessmentVersions }
      .add(versionToAdd)
  }

  fun getVersionsResponse() = VersionsResponse(
    this.versions.entries.sortedBy { it.key }
      .fold(object {
        val versionsMap = mutableMapOf<LocalDate, VersionsOnDate>()
        var lastAssessment: List<VersionDetails> = listOf()
        var lastPlan: List<VersionDetails> = listOf()
      }) { acc, (date, versionsOnDate) ->
        acc.apply {
          versionsMap[date] = VersionsOnDate(
            description = when {
              versionsOnDate.assessmentVersions.isNotEmpty() && versionsOnDate.planVersions.isNotEmpty() -> "Assessment and plan updated"
              versionsOnDate.assessmentVersions.isNotEmpty() -> "Assessment updated"
              else -> "Plan updated"
            },
            assessmentVersions = versionsOnDate.assessmentVersions.ifEmpty { lastAssessment.toMutableList() },
            planVersions = versionsOnDate.planVersions.ifEmpty { lastPlan.toMutableList() }
          )
          lastAssessment = versionsOnDate.assessmentVersions.sortedBy { it.updatedAt }.takeLast(1).ifEmpty { lastAssessment }
          lastPlan = versionsOnDate.planVersions.sortedBy { it.updatedAt }.takeLast(1).ifEmpty { lastPlan }
        }
      }.run {versionsMap}
      .toSortedMap(compareByDescending { it }),
  )
}
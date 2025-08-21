package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetailsList
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.time.LocalDate

class VersionsResponseFactory {
  val versions: MutableMap<LocalDate, VersionsOnDate> = mutableMapOf()

  fun addVersions(versionsToAdd: VersionDetailsList) = versionsToAdd.forEach { versionToAdd ->
    with(this.versions.getOrPut(versionToAdd.updatedAt.toLocalDate()) { VersionsOnDate() }) {
      val isCountersigned = versionToAdd.status == "COUNTERSIGNED"
      val versionsListOnThisDay = when (versionToAdd.entityType) {
        EntityType.PLAN -> if (isCountersigned) countersignedPlanVersions else planVersions
        EntityType.ASSESSMENT -> if (isCountersigned) countersignedAssessmentVersions else assessmentVersions
      }
      versionsListOnThisDay.add(versionToAdd)
    }
  }

  private fun processCountersignedVersions(
    countersignedVersions: MutableList<VersionDetails>,
    allVersionsOnThisDay: MutableList<VersionDetails>,
    resultCountersignedVersionsList: MutableList<VersionDetails>,
  ): VersionDetails? = countersignedVersions.maxByOrNull { it.updatedAt }?.also { latest ->
    resultCountersignedVersionsList.add(latest)
    allVersionsOnThisDay.removeAll(countersignedVersions)
  }

  private fun setCountersignedDescription(resultVersionsOnDate: VersionsOnDate) {
    with(resultVersionsOnDate) {
      countersignedDescription = when {
        countersignedAssessmentVersions.isNotEmpty() && countersignedPlanVersions.isNotEmpty() -> "Assessment and plan updated"
        countersignedAssessmentVersions.isNotEmpty() -> "Assessment updated"
        countersignedPlanVersions.isNotEmpty() -> "Plan updated"
        else -> null
      }
    }
  }

  private fun hasUpdates(
    hasCounterSigned: Boolean,
    allVersionsOnThisDay: MutableList<VersionDetails>,
    latestCountersigned: VersionDetails?,
    otherEntityCountersigned: VersionDetails?,
    otherEntityAllVersions: MutableList<VersionDetails>,
  ): Boolean = when {
    !hasCounterSigned -> allVersionsOnThisDay.isNotEmpty()

    latestCountersigned != null &&
      allVersionsOnThisDay.any {
        it.updatedAt.isAfter(latestCountersigned.updatedAt)
      } -> true

    hasCounterSigned &&
      otherEntityCountersigned != null &&
      otherEntityAllVersions.any {
        it.updatedAt.isAfter(otherEntityCountersigned.updatedAt)
      } -> true

    hasCounterSigned && otherEntityCountersigned == null && otherEntityAllVersions.isNotEmpty() -> true

    else -> false
  }

  private fun processRegularVersions(
    allVersionsOnThisDay: MutableList<VersionDetails>,
    countersignedVersions: MutableList<VersionDetails>,
    resultRegularList: MutableList<VersionDetails>,
    lastVersions: List<VersionDetails>,
    otherEntityAllVersions: MutableList<VersionDetails>,
    otherEntityCountersigned: MutableList<VersionDetails>,
  ): List<VersionDetails> = when {
    countersignedVersions.isNotEmpty() -> {
      val latestCountersigned = countersignedVersions.maxByOrNull { it.updatedAt }!!
      val versionsAfterCountersigned = allVersionsOnThisDay.filter { it.updatedAt.isAfter(latestCountersigned.updatedAt) }

      when {
        versionsAfterCountersigned.isNotEmpty() -> {
          val latest = versionsAfterCountersigned.maxByOrNull { it.updatedAt }!!
          resultRegularList.add(latest)
          listOf(latest)
        }
        shouldAddCountersignedToRegularList(otherEntityCountersigned, otherEntityAllVersions) -> {
          resultRegularList.add(latestCountersigned)
          listOf(latestCountersigned)
        }
        else -> listOf(latestCountersigned)
      }
    }
    allVersionsOnThisDay.isNotEmpty() -> {
      val latest = allVersionsOnThisDay.maxByOrNull { it.updatedAt }!!
      resultRegularList.add(latest)
      listOf(latest)
    }
    else -> {
      resultRegularList.addAll(lastVersions)
      lastVersions
    }
  }

  private fun shouldAddCountersignedToRegularList(
    countersignedVersions: MutableList<VersionDetails>,
    allVersions: MutableList<VersionDetails>,
  ): Boolean = when {
    countersignedVersions.isEmpty() -> allVersions.isNotEmpty()
    else -> {
      val latestOther = countersignedVersions.maxByOrNull { it.updatedAt }!!
      allVersions.any { it.updatedAt.isAfter(latestOther.updatedAt) }
    }
  }

  fun getVersionsResponse() = VersionsResponse(
    this.versions.entries.sortedBy { it.key }
      .fold(object {
        val versionsMap = mutableMapOf<LocalDate, VersionsOnDate>()
        var lastAssessment: List<VersionDetails> = listOf()
        var lastPlan: List<VersionDetails> = listOf()
      }) { acc, (date, versionsOnDate) ->
        acc.apply {
          val allAssessmentsOnThisDay = (versionsOnDate.assessmentVersions + versionsOnDate.countersignedAssessmentVersions).toMutableList()
          val allPlansOnThisDay = (versionsOnDate.planVersions + versionsOnDate.countersignedPlanVersions).toMutableList()

          val resultVersionsOnDate = VersionsOnDate()

          val latestCountersignedPlan = processCountersignedVersions(
            versionsOnDate.countersignedPlanVersions,
            allPlansOnThisDay,
            resultVersionsOnDate.countersignedPlanVersions,
          )

          val latestCountersignedAssessment = processCountersignedVersions(
            versionsOnDate.countersignedAssessmentVersions,
            allAssessmentsOnThisDay,
            resultVersionsOnDate.countersignedAssessmentVersions,
          )

          latestCountersignedPlan?.let { lastPlan = listOf(it) }
          latestCountersignedAssessment?.let { lastAssessment = listOf(it) }

          setCountersignedDescription(resultVersionsOnDate)

          val hasAssessmentUpdates = hasUpdates(
            resultVersionsOnDate.countersignedAssessmentVersions.isNotEmpty(),
            allAssessmentsOnThisDay,
            latestCountersignedAssessment,
            latestCountersignedPlan,
            allPlansOnThisDay,
          )

          val hasPlanUpdates = hasUpdates(
            resultVersionsOnDate.countersignedPlanVersions.isNotEmpty(),
            allPlansOnThisDay,
            latestCountersignedPlan,
            latestCountersignedAssessment,
            allAssessmentsOnThisDay,
          )

          resultVersionsOnDate.description = when {
            hasAssessmentUpdates && hasPlanUpdates -> "Assessment and plan updated"
            hasAssessmentUpdates -> "Assessment updated"
            hasPlanUpdates -> "Plan updated"
            else -> null
          }

          lastPlan = processRegularVersions(
            allPlansOnThisDay,
            resultVersionsOnDate.countersignedPlanVersions,
            resultVersionsOnDate.planVersions,
            lastPlan,
            allAssessmentsOnThisDay,
            resultVersionsOnDate.countersignedAssessmentVersions,
          )

          lastAssessment = processRegularVersions(
            allAssessmentsOnThisDay,
            resultVersionsOnDate.countersignedAssessmentVersions,
            resultVersionsOnDate.assessmentVersions,
            lastAssessment,
            allPlansOnThisDay,
            resultVersionsOnDate.countersignedPlanVersions,
          )
          versionsMap[date] = resultVersionsOnDate
        }
      }.run { versionsMap }
      .toSortedMap(),
  )
}

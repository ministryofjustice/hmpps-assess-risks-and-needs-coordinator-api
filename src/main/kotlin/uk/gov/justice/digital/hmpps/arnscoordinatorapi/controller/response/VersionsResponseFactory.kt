package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetailsList
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.time.LocalDate

class VersionsResponseFactory {
  val versions: MutableMap<LocalDate, VersionsOnDate> = mutableMapOf()

  fun addVersions(versionsToAdd: VersionDetailsList) = versionsToAdd.forEach { versionToAdd ->
    this.versions.getOrPut(versionToAdd.updatedAt.toLocalDate()) { VersionsOnDate() }
      .let { versionOnDate ->
        when {
          versionToAdd.entityType == EntityType.PLAN && versionToAdd.status == "COUNTERSIGNED" -> versionOnDate.countersignedPlanVersions.add(versionToAdd)
          versionToAdd.entityType == EntityType.ASSESSMENT && versionToAdd.status == "COUNTERSIGNED" -> versionOnDate.countersignedAssessmentVersions.add(versionToAdd)
          versionToAdd.entityType == EntityType.PLAN -> versionOnDate.planVersions.add(versionToAdd)
          versionToAdd.entityType == EntityType.ASSESSMENT -> versionOnDate.assessmentVersions.add(versionToAdd)
        }
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

          val countersignedPlans = versionsOnDate.countersignedPlanVersions
          if (countersignedPlans.isNotEmpty()) {
            val latestCountersignedPlan = countersignedPlans.maxByOrNull { it.updatedAt }!!
            resultVersionsOnDate.countersignedPlanVersions.add(latestCountersignedPlan)

            allPlansOnThisDay.removeAll(countersignedPlans)

            lastPlan = listOf(latestCountersignedPlan)
          }

          val countersignedAssessments = versionsOnDate.countersignedAssessmentVersions
          if (countersignedAssessments.isNotEmpty()) {
            val latestCountersignedAssessment = countersignedAssessments.maxByOrNull { it.updatedAt }!!
            resultVersionsOnDate.countersignedAssessmentVersions.add(latestCountersignedAssessment)

            allAssessmentsOnThisDay.removeAll(countersignedAssessments)

            lastAssessment = listOf(latestCountersignedAssessment)
          }

          if (resultVersionsOnDate.countersignedAssessmentVersions.isNotEmpty() && resultVersionsOnDate.countersignedPlanVersions.isNotEmpty()) {
            resultVersionsOnDate.countersignedDescription = "Assessment and plan updated"
          } else if (resultVersionsOnDate.countersignedAssessmentVersions.isNotEmpty()) {
            resultVersionsOnDate.countersignedDescription = "Assessment updated"
          } else if (resultVersionsOnDate.countersignedPlanVersions.isNotEmpty()) {
            resultVersionsOnDate.countersignedDescription = "Plan updated"
          }

          val latestCountersignedPlan = resultVersionsOnDate.countersignedPlanVersions.maxByOrNull { it.updatedAt }
          val hasAssessmentUpdates = (resultVersionsOnDate.countersignedAssessmentVersions.isEmpty() && allAssessmentsOnThisDay.isNotEmpty()) ||
            (resultVersionsOnDate.countersignedAssessmentVersions.isNotEmpty() && allAssessmentsOnThisDay.filter { it.updatedAt.isAfter(resultVersionsOnDate.countersignedAssessmentVersions.maxByOrNull { it.updatedAt }!!.updatedAt) }.isNotEmpty()) ||
            (resultVersionsOnDate.countersignedAssessmentVersions.isNotEmpty() && latestCountersignedPlan != null && allPlansOnThisDay.any { it.updatedAt.isAfter(latestCountersignedPlan.updatedAt) }) ||
            (resultVersionsOnDate.countersignedAssessmentVersions.isNotEmpty() && latestCountersignedPlan == null && allPlansOnThisDay.isNotEmpty())

          val latestCountersignedAssessment = resultVersionsOnDate.countersignedAssessmentVersions.maxByOrNull { it.updatedAt }
          val hasPlanUpdates = (resultVersionsOnDate.countersignedPlanVersions.isEmpty() && allPlansOnThisDay.isNotEmpty()) ||
            (resultVersionsOnDate.countersignedPlanVersions.isNotEmpty() && allPlansOnThisDay.filter { it.updatedAt.isAfter(resultVersionsOnDate.countersignedPlanVersions.maxByOrNull { it.updatedAt }!!.updatedAt) }.isNotEmpty()) ||
            (resultVersionsOnDate.countersignedPlanVersions.isNotEmpty() && latestCountersignedAssessment != null && allAssessmentsOnThisDay.any { it.updatedAt.isAfter(latestCountersignedAssessment.updatedAt) }) ||
            (resultVersionsOnDate.countersignedPlanVersions.isNotEmpty() && latestCountersignedAssessment == null && allAssessmentsOnThisDay.isNotEmpty())

          resultVersionsOnDate.description = when {
            hasAssessmentUpdates && hasPlanUpdates -> "Assessment and plan updated"
            hasAssessmentUpdates -> "Assessment updated"
            hasPlanUpdates -> "Plan updated"
            else -> null
          }

          if (resultVersionsOnDate.countersignedPlanVersions.isNotEmpty()) {
            val latestCountersignedPlan = resultVersionsOnDate.countersignedPlanVersions.maxByOrNull { it.updatedAt }!!
            val plansAfterCountersigned = allPlansOnThisDay.filter { it.updatedAt.isAfter(latestCountersignedPlan.updatedAt) }

            if (plansAfterCountersigned.isNotEmpty()) {
              val latestPlan = plansAfterCountersigned.maxByOrNull { it.updatedAt }!!
              resultVersionsOnDate.planVersions.add(latestPlan)
              lastPlan = listOf(latestPlan)
            } else {
              lastPlan = listOf(latestCountersignedPlan)
            }
          } else if (allPlansOnThisDay.isNotEmpty()) {
            val latestPlan = allPlansOnThisDay.maxByOrNull { it.updatedAt }!!
            resultVersionsOnDate.planVersions.add(latestPlan)
            lastPlan = listOf(latestPlan)
          } else {
            if (lastPlan.isNotEmpty()) {
              lastPlan.forEach { resultVersionsOnDate.planVersions.add(it) }
            }
          }

          if (resultVersionsOnDate.countersignedAssessmentVersions.isNotEmpty()) {
            val latestCountersignedAssessment = resultVersionsOnDate.countersignedAssessmentVersions.maxByOrNull { it.updatedAt }!!
            val assessmentsAfterCountersigned = allAssessmentsOnThisDay.filter { it.updatedAt.isAfter(latestCountersignedAssessment.updatedAt) }

            if (assessmentsAfterCountersigned.isNotEmpty()) {
              val latestAssessment = assessmentsAfterCountersigned.maxByOrNull { it.updatedAt }!!
              resultVersionsOnDate.assessmentVersions.add(latestAssessment)
              lastAssessment = listOf(latestAssessment)
            } else {
              lastAssessment = listOf(latestCountersignedAssessment)

              val shouldAddAssessment = if (resultVersionsOnDate.countersignedPlanVersions.isEmpty()) {
                allPlansOnThisDay.isNotEmpty()
              } else {
                val latestCoutersignedPlan = resultVersionsOnDate.countersignedPlanVersions.maxByOrNull { it.updatedAt }!!
                allPlansOnThisDay.any { it.updatedAt.isAfter(latestCoutersignedPlan.updatedAt) }
              }

              if (shouldAddAssessment) {
                resultVersionsOnDate.assessmentVersions.add(latestCountersignedAssessment)
              }
            }
          } else if (allAssessmentsOnThisDay.isNotEmpty()) {
            val latestAssessment = allAssessmentsOnThisDay.maxByOrNull { it.updatedAt }!!
            resultVersionsOnDate.assessmentVersions.add(latestAssessment)
            lastAssessment = listOf(latestAssessment)
          } else {
            if (lastAssessment.isNotEmpty()) {
              lastAssessment.forEach { resultVersionsOnDate.assessmentVersions.add(it) }
            }
          }
          versionsMap[date] = resultVersionsOnDate
        }
      }.run { versionsMap }
      .toSortedMap(),
  )
}

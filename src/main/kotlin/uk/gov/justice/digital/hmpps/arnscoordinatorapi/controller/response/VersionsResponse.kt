package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import java.time.LocalDate
import java.util.Collections.emptySortedMap
import java.util.SortedMap

data class VersionsOnDate(
  var assessmentVersions: MutableList<VersionDetails> = mutableListOf(),
  var planVersions: MutableList<VersionDetails> = mutableListOf(),
)

data class LastVersionsOnDate(
  var description: String? = null,
  var assessmentVersion: VersionDetails? = null,
  var planVersion: VersionDetails? = null,
)

typealias VersionsTable = SortedMap<LocalDate, LastVersionsOnDate>

data class VersionsResponse(
  val allVersions: VersionsTable = emptySortedMap(),
  val countersignedVersions: VersionsTable = emptySortedMap(),
)

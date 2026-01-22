package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import java.time.LocalDate
import java.util.Collections.emptySortedMap
import java.util.SortedMap

data class VersionsOnDate(
  val assessmentVersions: MutableList<VersionDetails> = mutableListOf(),
  val planVersions: MutableList<VersionDetails> = mutableListOf(),
)

data class LastVersionsOnDate(
  val description: String? = null,
  val assessmentVersion: VersionDetails? = null,
  val planVersion: VersionDetails? = null,
)

typealias VersionsTable = SortedMap<LocalDate, LastVersionsOnDate>

data class VersionsResponse(
  val allVersions: VersionsTable = emptySortedMap(),
  val countersignedVersions: VersionsTable = emptySortedMap(),
)

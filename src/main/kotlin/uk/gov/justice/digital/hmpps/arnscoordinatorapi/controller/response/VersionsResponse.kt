package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import java.time.LocalDate
import java.util.SortedMap

data class VersionsOnDate(
  var description: String? = null,
  var assessmentVersions: MutableList<VersionDetails> = mutableListOf(),
  var planVersions: MutableList<VersionDetails> = mutableListOf(),
)

data class VersionsResponse(
  var versions: SortedMap<LocalDate, VersionsOnDate>
)
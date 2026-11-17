package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

import java.time.LocalDateTime

data class PathwayChange(
  val version: VersionedEntity,
  val clearedFrom: LocalDateTime,
)

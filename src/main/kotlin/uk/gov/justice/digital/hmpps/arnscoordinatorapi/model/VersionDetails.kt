package uk.gov.justice.digital.hmpps.arnscoordinatorapi.model

import java.time.LocalDateTime
import java.util.UUID

data class VersionDetails(
  val uuid: UUID,
  val version: Long,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val status: String,
  val planAgreementStatus: String? = null,
  val entityType: EntityType,
)

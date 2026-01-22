package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.time.LocalDateTime
import java.util.UUID

typealias VersionDetailsList = List<VersionDetails>

data class VersionDetails(
  val uuid: UUID,
  val version: Int,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val status: String,
  val planAgreementStatus: String?,
  val entityType: EntityType,
)

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.versioning.persistence.OasysEvent
import java.time.LocalDateTime
import java.util.UUID

data class VersionedEntity(
  val id: UUID,
  val version: Long,
  val entityType: EntityType,
  val deleted: Boolean = false,
  val updatedAt: LocalDateTime? = null,
)

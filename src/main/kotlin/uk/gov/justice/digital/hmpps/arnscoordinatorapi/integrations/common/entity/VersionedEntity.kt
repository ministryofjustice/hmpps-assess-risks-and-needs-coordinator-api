package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

data class VersionedEntity(
  val id: UUID,
  val version: Long,
  val entityType: EntityType,
)

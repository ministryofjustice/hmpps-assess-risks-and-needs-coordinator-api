package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import java.util.UUID

interface EntityStrategy {
  val entityType: EntityType

  fun create(createData: CreateData): OperationResult<VersionedEntity>

  fun delete(entityUuid: UUID): OperationResult<Unit>
}

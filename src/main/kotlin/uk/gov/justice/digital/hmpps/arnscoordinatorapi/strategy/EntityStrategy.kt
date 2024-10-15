package uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.LockData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import java.util.UUID

interface EntityStrategy {
  val entityType: EntityType

  fun create(createData: CreateData): OperationResult<VersionedEntity>

  fun associateWithPrevious(oasysAssessmentPk: String, associations: List<OasysAssociation>): OperationResult<OasysAssociation>

  fun fetch(entityUuid: UUID): OperationResult<*>

  fun sign(signData: SignData, entityUuid: UUID): OperationResult<VersionedEntity>

  fun lock(lockData: LockData, entityUuid: UUID): OperationResult<VersionedEntity>

  fun rollback(request: OasysRollbackRequest, entityUuid: UUID): OperationResult<VersionedEntity>

  fun delete(entityUuid: UUID): OperationResult<Unit>

  fun counterSign(entityUuid: UUID, request: OasysCounterSignRequest): OperationResult<VersionedEntity>
}

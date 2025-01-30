package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.*

class SignCommand(
  private val strategy: EntityStrategy,
  private val entityUuid: UUID,
  private val signData: SignData,
) : Command {

  override fun execute(): OperationResult<VersionedEntity> = strategy.sign(signData, entityUuid)

  fun rollback(): OperationResult<Unit> = OperationResult.Failure("Rollback has not been implemented for sign")
}

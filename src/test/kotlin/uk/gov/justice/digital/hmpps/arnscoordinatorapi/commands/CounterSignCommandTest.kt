package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionedEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class CounterSignCommandTest {
  private lateinit var entityStrategy: EntityStrategy
  private lateinit var command: CounterSignCommand
  private lateinit var request: OasysCounterSignRequest
  private val entityUuid: UUID = UUID.randomUUID()

  @BeforeEach
  fun setUp() {
    entityStrategy = mock()
    request = OasysCounterSignRequest(
      sanVersionNumber = 1,
      sentencePlanVersionNumber = 1,
      outcome = CounterSignOutcome.COUNTERSIGNED,
      userDetails = OasysUserDetails("1", "OASys User"),
    )
    command = CounterSignCommand(entityStrategy, entityUuid, request)
  }

  @Test
  fun `should execute fetch with correct UUID and return success result`() {
    val successfulRequest = OperationResult.Success(VersionedEntity(UUID.randomUUID(), 2, EntityType.ASSESSMENT))
    `when`(entityStrategy.counterSign(entityUuid, request)).thenReturn(successfulRequest)

    val result = command.execute()

    verify(entityStrategy).counterSign(entityUuid, request)

    assertEquals(successfulRequest, result)
  }

  @Test
  fun `should execute fetch and return failure result`() {
    val failureResult = OperationResult.Failure<Nothing>("Fetch failed")
    `when`(entityStrategy.counterSign(entityUuid, request)).thenReturn(failureResult)

    val result = command.execute()

    verify(entityStrategy).counterSign(entityUuid, request)

    assertEquals(failureResult, result)
  }
}

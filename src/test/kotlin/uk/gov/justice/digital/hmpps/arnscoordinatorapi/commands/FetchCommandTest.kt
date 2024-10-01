package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class FetchCommandTest {
  private lateinit var entityStrategy: EntityStrategy
  private lateinit var fetchCommand: FetchCommand
  private val entityUuid: UUID = UUID.randomUUID()

  @BeforeEach
  fun setUp() {
    entityStrategy = mock()
    fetchCommand = FetchCommand(entityStrategy, entityUuid) // Initialize FetchCommand with the mock
  }

  @Test
  fun `should execute fetch with correct UUID and return success result`() {
    val expectedResult = OperationResult.Success("Fetched Data")
    `when`(entityStrategy.fetch(entityUuid)).thenReturn(expectedResult)

    val result = fetchCommand.execute()

    verify(entityStrategy).fetch(entityUuid)

    assertEquals(expectedResult, result)
  }

  @Test
  fun `should execute fetch and return failure result`() {
    val failureResult = OperationResult.Failure<Nothing>("Fetch failed")
    `when`(entityStrategy.fetch(entityUuid)).thenReturn(failureResult)

    val result = fetchCommand.execute()

    verify(entityStrategy).fetch(entityUuid)

    assertEquals(failureResult, result)
  }
}

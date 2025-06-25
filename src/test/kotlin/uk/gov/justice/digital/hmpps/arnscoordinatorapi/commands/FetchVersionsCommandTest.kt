package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.OperationResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.VersionDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy
import java.util.UUID

class FetchVersionsCommandTest {
  private lateinit var entityStrategy: EntityStrategy
  private lateinit var fetchVersionsCommand: FetchVersionsCommand
  private val entityUuid: UUID = UUID.randomUUID()

  @BeforeEach
  fun setUp() {
    entityStrategy = mock()
    fetchVersionsCommand = FetchVersionsCommand(entityStrategy, entityUuid)
  }

  @Test
  fun `should execute fetch versions with correct UUID and return success result`() {
    val version: VersionDetails = mock()
    val expectedResult = OperationResult.Success(listOf(version))
    `when`(entityStrategy.fetchVersions(entityUuid)).thenReturn(expectedResult)

    val result = fetchVersionsCommand.execute()

    verify(entityStrategy).fetchVersions(entityUuid)

    assertEquals(expectedResult, result)
  }

  @Test
  fun `should execute fetch versions and return failure result`() {
    val failureResult = OperationResult.Failure<Nothing>("Fetch failed")
    `when`(entityStrategy.fetchVersions(entityUuid)).thenReturn(failureResult)

    val result = fetchVersionsCommand.execute()

    verify(entityStrategy).fetchVersions(entityUuid)

    assertEquals(failureResult, result)
  }
}

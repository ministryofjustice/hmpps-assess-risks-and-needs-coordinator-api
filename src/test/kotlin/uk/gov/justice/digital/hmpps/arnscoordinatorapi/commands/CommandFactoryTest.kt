package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy

class CommandFactoryTest {
  private val entityStrategy: EntityStrategy = mock()
  private lateinit var commandFactory: CommandFactory

  private val createData = CreateData(
    plan = CreatePlanData(PlanType.INITIAL, UserDetails("id", "name")),
    assessment = null,
  )

  @BeforeEach
  fun setup() {
    commandFactory = CommandFactory()
  }

  @Test
  fun `should create a CreateCommand with correct strategy and createData`() {
    val command = commandFactory.createCommand(entityStrategy, createData)

    assertNotNull(command)
  }
}

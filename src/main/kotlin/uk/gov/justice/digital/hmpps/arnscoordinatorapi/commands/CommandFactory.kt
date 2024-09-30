package uk.gov.justice.digital.hmpps.arnscoordinatorapi.commands

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.CreateData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.strategy.EntityStrategy

@Component
class CommandFactory {

  fun createCommand(entityStrategy: EntityStrategy, createData: CreateData): CreateCommand {
    return CreateCommand(entityStrategy, createData)
  }
}

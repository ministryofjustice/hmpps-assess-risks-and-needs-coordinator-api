package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = CommandSuccessCommandResult::class)
@JsonSubTypes(
  JsonSubTypes.Type(value = CommandSuccessCommandResult::class, name = "CommandSuccessCommandResult"),
)
sealed interface CommandResult {
  val success: Boolean
  val message: String
}

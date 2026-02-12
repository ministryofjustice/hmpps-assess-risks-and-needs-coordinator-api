package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.command

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = CreateAssessmentCommandResult::class, name = "CreateAssessmentCommandResult"),
  JsonSubTypes.Type(value = CommandSuccessResult::class, name = "CommandSuccessCommandResult"),
)
sealed interface CommandResult {
  val success: Boolean
  val message: String
}

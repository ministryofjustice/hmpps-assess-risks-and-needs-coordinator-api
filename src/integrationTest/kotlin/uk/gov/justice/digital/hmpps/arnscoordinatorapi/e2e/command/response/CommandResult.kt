package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request.UpdateAssessmentAnswersCommand

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = UpdateAssessmentAnswersCommand::class, name = "UpdateAssessmentAnswersCommand")
)
sealed interface CommandResult {
  val success: Boolean
  val message: String
}
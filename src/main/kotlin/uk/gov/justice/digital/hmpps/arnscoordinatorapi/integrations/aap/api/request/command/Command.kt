package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = CreateAssessmentCommand::class, name = "CreateAssessmentCommand"),
)
sealed interface Command {
  val user: AAPUser
}

package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(JsonSubTypes.Type(value = UpdateAssessmentAnswersCommand::class, name = "UpdateAssessmentAnswersCommand"))
interface Command

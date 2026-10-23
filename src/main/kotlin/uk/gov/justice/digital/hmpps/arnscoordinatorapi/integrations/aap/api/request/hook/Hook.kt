package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = UpdateOasysDataMapping::class, name = "UpdateOasysDataMapping"),
)
sealed interface Hook

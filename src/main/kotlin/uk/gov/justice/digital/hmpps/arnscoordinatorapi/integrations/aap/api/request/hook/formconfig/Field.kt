package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook.formconfig

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Field(
  val code: String,
  val options: List<Option> = emptyList(),
  val type: FieldType = FieldType.TEXT,
  val section: String = "",
  val collection: String = "",
)

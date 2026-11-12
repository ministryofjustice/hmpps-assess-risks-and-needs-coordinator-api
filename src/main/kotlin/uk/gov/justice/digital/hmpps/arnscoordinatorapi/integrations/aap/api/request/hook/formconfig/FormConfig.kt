package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook.formconfig

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class FormConfig(
  val version: String,
  val fields: Map<String, Field> = emptyMap(),
)

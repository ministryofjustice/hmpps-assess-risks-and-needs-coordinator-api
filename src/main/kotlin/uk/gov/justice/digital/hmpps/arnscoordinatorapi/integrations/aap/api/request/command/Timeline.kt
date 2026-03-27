package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command

data class Timeline(
  val type: String,
  val data: Map<String, Any> = emptyMap(),
)

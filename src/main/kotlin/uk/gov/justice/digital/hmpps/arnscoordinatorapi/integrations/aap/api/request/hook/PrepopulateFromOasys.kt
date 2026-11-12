package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook

data class PrepopulateFromOasys(
  val data: Map<String, Any?>,
) : Hook

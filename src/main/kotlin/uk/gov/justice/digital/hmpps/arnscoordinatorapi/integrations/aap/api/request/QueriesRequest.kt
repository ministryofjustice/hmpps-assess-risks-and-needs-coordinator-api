package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request

data class QueriesRequest(
  val queries: List<Any>,
) {
  companion object {
    fun of(vararg queries: Any) = QueriesRequest(queries.toList())
  }
}

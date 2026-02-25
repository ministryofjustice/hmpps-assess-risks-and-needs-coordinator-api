package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query

data class QueriesRequest(
  val queries: List<Query>,
) {
  companion object {
    fun of(vararg queries: Query) = QueriesRequest(queries.toList())
  }
}

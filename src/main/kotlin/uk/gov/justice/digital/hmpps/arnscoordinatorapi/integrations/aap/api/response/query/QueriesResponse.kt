package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.Query

data class QueriesResponse(
  val queries: List<QueryResponse>,
) {
  inline fun <reified T : QueryResult> extractNthInstance(index: Int): T = queries.map { it.result }
    .filterIsInstance<T>().getOrNull(index)
    ?: error("No response found at index $index for ${T::class.simpleName}")

  inline fun <reified T : QueryResult> extractSingle(): T = queries.map { it.result }
    .filterIsInstance<T>()
    .singleOrNull()
    ?: error("Expected exactly one ${T::class.simpleName}")
}

data class QueryResponse(
  val request: Query,
  val result: QueryResult,
)

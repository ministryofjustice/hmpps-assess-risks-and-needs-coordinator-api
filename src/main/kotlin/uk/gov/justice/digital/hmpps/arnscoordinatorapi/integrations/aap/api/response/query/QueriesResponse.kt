package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query.Query

data class QueriesResponse(
  val queries: List<QueryResponse>,
)

data class QueryResponse(
  val request: Query,
  val result: QueryResult,
)

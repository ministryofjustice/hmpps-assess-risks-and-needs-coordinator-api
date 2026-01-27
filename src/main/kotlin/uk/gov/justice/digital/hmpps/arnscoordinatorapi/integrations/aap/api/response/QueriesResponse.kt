package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response

data class QueriesResponse(
  val queries: List<QueryResponse>,
)

data class QueryResponse(
  val result: AssessmentVersionQueryResult,
)

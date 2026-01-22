package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response

import java.time.LocalDateTime
import java.util.UUID

typealias PlanVersionsResponse = List<PlanVersionDetails>

data class PlanVersionDetails(
  val uuid: UUID,
  val version: Int,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
  val status: String,
  val agreementStatus: String,
)

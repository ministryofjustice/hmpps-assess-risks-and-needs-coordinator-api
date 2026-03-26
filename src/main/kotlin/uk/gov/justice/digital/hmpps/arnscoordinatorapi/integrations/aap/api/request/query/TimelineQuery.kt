package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AssessmentIdentifier
import java.time.LocalDateTime

data class TimelineQuery(
  override val user: AAPUser,
  override val assessmentIdentifier: AssessmentIdentifier,
  override val timestamp: LocalDateTime? = null,
  val pageNumber: Int = 0,
  val pageSize: Int = 50,
  val subject: AAPUser? = null,
  val from: LocalDateTime? = null,
  val to: LocalDateTime? = null,
  val includeEventTypes: Set<String>? = null,
  val excludeEventTypes: Set<String>? = null,
  val includeCustomTypes: Set<String>? = null,
  val excludeCustomTypes: Set<String>? = null,
) : Query

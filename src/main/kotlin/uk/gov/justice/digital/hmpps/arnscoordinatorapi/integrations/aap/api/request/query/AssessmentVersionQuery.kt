package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.query

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AAPUser
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.AssessmentIdentifier
import java.time.LocalDateTime

data class AssessmentVersionQuery(
  override val user: AAPUser,
  override val assessmentIdentifier: AssessmentIdentifier,
  override val timestamp: LocalDateTime? = null,
) : Query

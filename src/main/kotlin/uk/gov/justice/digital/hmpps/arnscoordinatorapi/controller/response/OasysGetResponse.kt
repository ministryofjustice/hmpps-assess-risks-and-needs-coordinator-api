package uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response

import com.fasterxml.jackson.annotation.JsonInclude
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.plan.PlanState
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.plan.PlanType
import java.time.LocalDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OasysGetResponse(
  override var sanAssessmentId: UUID = UUID(0, 0),
  override var sanAssessmentVersion: Long = 0,
  var sanAssessmentData: Map<*, *>? = null,
  var sanOasysEquivalent: Map<String, *>? = null,
  var lastUpdatedTimestampSAN: LocalDateTime? = null,

  override var sentencePlanId: UUID = UUID(0, 0),
  override var sentencePlanVersion: Long = 0,
  var planComplete: PlanState = PlanState.INCOMPLETE,
  var planType: PlanType? = null,
  var lastUpdatedTimestampSP: LocalDateTime? = null,
) : OasysVersionedEntityResponse(
  sanAssessmentId = sanAssessmentId,
  sanAssessmentVersion = sanAssessmentVersion,
  sentencePlanId = sentencePlanId,
  sentencePlanVersion = sentencePlanVersion,
) {

  companion object {
    // Downstream APIs HTML-escape free text answers; this reverses that so consumers see the original characters.
    fun unescapeHtmlEntities(raw: String): String = raw
      .replace("&amp;", "&")
      .replace("&#x27;", "'")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .replace("&#x2F;", "/")
      .replace("&#x5C;", "\\\\")
      .replace("&#96;", "`")
      .replace("&quot;", "\\\"")
  }
}

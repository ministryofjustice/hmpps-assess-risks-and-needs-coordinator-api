package uk.gov.justice.digital.hmpps.arnscoordinatorapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.PropertyValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.SingleValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.plan.PlanState
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.plan.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception.CoordinatorException
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.UpdateAssessmentPropertiesCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.AAPAssessmentType
import java.time.ZoneOffset
import java.util.UUID

@Service
class SentencePlanService {

  fun create(request: OasysCreateRequest) = CreateAssessmentCommand(
    assessmentType = AAPAssessmentType.SENTENCE_PLAN.name,
    formVersion = "v1.0",
    properties = mapOf(
      "PLAN_TYPE" to PropertyValue(type = "Single", value = request.planType.name),
    ),
    identifiers = request.identifiers(),
    flags = request.assessmentType.toFlags(),
    user = request.userDetails.toAAPUser(),
  )

  fun fetch(data: AssessmentVersionQueryResult, response: OasysGetResponse) {
    response.sentencePlanId = data.assessmentUuid
    response.sentencePlanVersion = data.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli()
    response.planType = (data.properties["PLAN_TYPE"] as? SingleValue)?.let { PlanType.valueOf(it.value) }
      ?: throw CoordinatorException("No value for PLAN_TYPE for entity ${data.assessmentUuid}")
    response.planComplete = data.collections
      .find { it.name == "PLAN_AGREEMENTS" }
      ?.items
      ?.maxByOrNull { it.updatedAt }
      ?.let { (it.properties["status"] as? SingleValue) }
      ?.value
      ?.takeIf { it != "DRAFT" }
      ?.let { PlanState.COMPLETE }
      ?: PlanState.INCOMPLETE
    response.lastUpdatedTimestampSP = data.updatedAt
  }

  fun reset(request: OasysCreateRequest, entityUuid: UUID) = UpdateAssessmentPropertiesCommand(
    assessmentUuid = entityUuid,
    user = request.userDetails.toAAPUser(),
    hooks = listOf(), // TODO: add ze hook
  )
}

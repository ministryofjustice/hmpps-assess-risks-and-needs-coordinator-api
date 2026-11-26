package uk.gov.justice.digital.hmpps.arnscoordinatorapi.service

import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook.UpdateOasysDataMapping
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook.formconfig.FormConfig
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.AssessmentVersionQueryResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.SingleValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysGetResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.CreateAssessmentCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.AAPAssessmentType
import java.time.ZoneOffset

@Service
class StrengthsAndNeedsService(
  private val objectMapper: ObjectMapper,
) {
  fun create(request: OasysCreateRequest): CreateAssessmentCommand {
    val formVersion = "v1.0"
    return CreateAssessmentCommand(
      assessmentType = AAPAssessmentType.STRENGTHS_AND_NEEDS.name,
      formVersion = formVersion,
      identifiers = request.identifiers(),
      flags = request.assessmentType.toFlags(),
      user = request.userDetails.toAAPUser(),
      hooks = listOf(
        UpdateOasysDataMapping(
          formConfig = objectMapper.readValue(
            requireNotNull(javaClass.getResourceAsStream("/san/formconfig/$formVersion.json")) {
              "Missing form config fixture: src/main/resources/san/formconfig/$formVersion.json"
            },
            FormConfig::class.java,
          ),
        ),
      ),
    )
  }

  fun fetch(data: AssessmentVersionQueryResult, response: OasysGetResponse) {
    response.sanAssessmentId = data.assessmentUuid
    response.sanAssessmentVersion = data.updatedAt.toInstant(ZoneOffset.UTC).toEpochMilli()
    response.sanAssessmentData = objectMapper.readValue<Map<String, *>>(
      OasysGetResponse.unescapeHtmlEntities(
        objectMapper.writeValueAsString(
          data.properties.filterKeys { it.endsWith("_section_complete") } + data.answers,
        ),
      ),
    )
    response.sanOasysEquivalent = (data.properties["oasys_equivalent"] as? SingleValue)
      ?.value
      ?.let { objectMapper.readValue<Map<String, *>>(OasysGetResponse.unescapeHtmlEntities(it)) }
      ?: emptyMap<String, Any>()
    response.lastUpdatedTimestampSAN = data.updatedAt
  }
}

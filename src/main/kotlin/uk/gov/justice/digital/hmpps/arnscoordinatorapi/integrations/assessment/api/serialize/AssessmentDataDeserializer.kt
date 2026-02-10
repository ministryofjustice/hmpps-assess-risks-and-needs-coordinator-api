package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.serialize

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.AssessmentData

class AssessmentDataDeserializer : ValueDeserializer<AssessmentData>() {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): AssessmentData? = context.readTree(parser).toString()
    .replace("&amp;", "&")
    .replace("&#x27;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&#x2F;", "/")
    .replace("&#x5C;", "\\\\")
    .replace("&#96;", "`")
    .replace("&quot;", "\\\"")
    .let {
      JsonMapper().readValue(it, AssessmentData::class.java)
    }
}

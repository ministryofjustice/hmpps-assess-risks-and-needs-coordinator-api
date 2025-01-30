package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.serialize

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.AssessmentData

class AssessmentDataDeserializer : JsonDeserializer<AssessmentData>() {
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

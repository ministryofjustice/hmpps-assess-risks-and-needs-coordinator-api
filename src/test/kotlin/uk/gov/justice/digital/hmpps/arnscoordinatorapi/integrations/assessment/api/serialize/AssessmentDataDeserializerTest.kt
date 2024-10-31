package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.serialize

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.response.AssessmentData
import java.util.stream.Stream

class AssessmentDataDeserializerTest {
  @ParameterizedTest
  @MethodSource("dataProvider")
  fun `should deserialize and replace escaped characters`(json: String, expected: AssessmentData) {
    val parser: JsonParser = mock()
    val context: DeserializationContext = mock()
    val node: JsonNode = mock()

    `when`(node.toString()).thenReturn(json)
    `when`(context.readTree(parser)).thenReturn(node)

    assertEquals(AssessmentDataDeserializer().deserialize(parser, context), expected)

    verify(context).readTree(parser)
  }

  companion object {
    @JvmStatic
    fun dataProvider(): Stream<Arguments> {
      val escapedCharacters = mapOf(
        "&amp;" to "&",
        "&#x27;" to "'",
        "&lt;" to "<",
        "&gt;" to ">",
        "&#x2F;" to "/",
        "&#x5C;" to "\\",
        "&#96;" to "`",
        "&quot;" to "\"",
      )

      return Stream.of(
        arguments("""{}""", emptyMap<String, Any>()),
        arguments("""{"test": "val"}""", mapOf("test" to "val")),
        arguments("""{"test": "val", "deep": { "nesting": "ok" }}""", mapOf("test" to "val", "deep" to mapOf("nesting" to "ok"))),
        *escapedCharacters.map {
          arguments("""{"test": "${it.key}"}""", mapOf("test" to it.value))
        }.toTypedArray(),
      )
    }
  }
}

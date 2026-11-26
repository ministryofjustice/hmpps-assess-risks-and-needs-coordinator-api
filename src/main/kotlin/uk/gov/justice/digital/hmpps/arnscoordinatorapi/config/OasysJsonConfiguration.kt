package uk.gov.justice.digital.hmpps.arnscoordinatorapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ResolvableType
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import tools.jackson.core.json.JsonWriteFeature
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysGetResponse

@Configuration
class OasysJsonConfiguration {
  // OASys decodes responses as Windows-1252, so non-ASCII text is sent escaped.
  @Bean
  fun oasysGetResponseConverter(jsonMapper: JsonMapper): HttpMessageConverter<Any> {
    val oasysMapper = jsonMapper.rebuild()
      .enable(JsonWriteFeature.ESCAPE_NON_ASCII)
      .build()

    return object : JacksonJsonHttpMessageConverter(oasysMapper) {
      override fun canRead(type: ResolvableType, mediaType: MediaType?) = false

      override fun canWrite(targetType: ResolvableType, valueClass: Class<*>, mediaType: MediaType?) = OasysGetResponse::class.java.isAssignableFrom(valueClass) &&
        super.canWrite(targetType, valueClass, mediaType)
    }
  }
}

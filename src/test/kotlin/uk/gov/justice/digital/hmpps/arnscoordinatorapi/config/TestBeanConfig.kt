package uk.gov.justice.digital.hmpps.arnscoordinatorapi.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.RandomOasysPk

@TestConfiguration
class TestBeanConfig {
  @Bean
  fun randomOasysPk() = RandomOasysPk()
}

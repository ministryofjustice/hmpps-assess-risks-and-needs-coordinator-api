package uk.gov.justice.digital.hmpps.arnscoordinatorapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime

class Clock {
  fun now() = Clock.now()

  companion object {
    fun now(): LocalDateTime = LocalDateTime.now()
  }
}

@Configuration
class ClockConfiguration {
  @Bean
  fun clock() = Clock()
}

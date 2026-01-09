package uk.gov.justice.digital.hmpps.arnscoordinatorapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class Clock {
  fun now() = Clock.now()

  companion object {
    fun now(): LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
  }
}

@Configuration
class ClockConfiguration {
  @Bean
  fun clock() = Clock()
}

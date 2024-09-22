package uk.gov.justice.digital.hmpps.arnscoordinatorapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HmppsAssessRisksAndNeedsCoordinatorAPI

fun main(args: Array<String>) {
  runApplication<HmppsAssessRisksAndNeedsCoordinatorAPI>(*args)
}
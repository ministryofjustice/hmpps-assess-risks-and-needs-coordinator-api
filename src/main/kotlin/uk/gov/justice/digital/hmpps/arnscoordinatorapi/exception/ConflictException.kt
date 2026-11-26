package uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception

import org.springframework.http.HttpStatus

class ConflictException(message: String) :
  CoordinatorException(
    message = message,
    developerMessage = message,
    statusCode = HttpStatus.CONFLICT,
  )

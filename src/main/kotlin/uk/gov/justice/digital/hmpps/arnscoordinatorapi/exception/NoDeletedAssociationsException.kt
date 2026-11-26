package uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception

import org.springframework.http.HttpStatus

class NoDeletedAssociationsException(oasysPk: String): CoordinatorException(
  message = "No deleted associations found for PK $oasysPk",
  statusCode = HttpStatus.NOT_FOUND,
)

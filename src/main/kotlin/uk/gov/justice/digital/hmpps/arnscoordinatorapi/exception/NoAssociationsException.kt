package uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception

import org.springframework.http.HttpStatus

class NoAssociationsException(vararg oasysPks: String): CoordinatorException(
  message = if (oasysPks.size == 1) "No associations found for PK ${oasysPks.first()}"
    else "The following association(s) could not be located: ${oasysPks.joinToString()} and the operation has not been actioned.",
  statusCode = HttpStatus.NOT_FOUND,
)

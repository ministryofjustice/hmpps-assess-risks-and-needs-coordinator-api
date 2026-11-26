package uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception

import org.springframework.http.HttpStatus
import java.util.UUID

class NoAssociationsForEntityUuidException(entityUuid: UUID): CoordinatorException(
  message = "No associations found for entity $entityUuid",
  developerMessage = "No associations found for entity $entityUuid",
  statusCode = HttpStatus.NOT_FOUND,
)

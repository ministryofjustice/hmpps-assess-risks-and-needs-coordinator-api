package uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception

import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.EntityType

class NoAssociationsForEntityException(entityType: EntityType, oasysPk: String) :
  CoordinatorException(
    message = "No $entityType association found for PK $oasysPk",
    developerMessage = "No $entityType association found for PK $oasysPk",
    statusCode = HttpStatus.NOT_FOUND,
  )

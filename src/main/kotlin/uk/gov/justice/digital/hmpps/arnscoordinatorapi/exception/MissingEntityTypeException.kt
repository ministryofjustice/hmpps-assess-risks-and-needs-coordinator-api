package uk.gov.justice.digital.hmpps.arnscoordinatorapi.exception

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysAssociationEntity

class MissingEntityTypeException(association: OasysAssociationEntity) :
  CoordinatorException(
    message = "Missing entity type for association ${association.uuid}",
  )

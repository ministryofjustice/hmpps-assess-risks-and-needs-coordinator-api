package uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType

enum class AssessmentType(val entityTypes: Set<EntityType>) {
  SAN_SP(setOf(EntityType.ASSESSMENT, EntityType.AAP_PLAN)),
  SP(setOf(EntityType.AAP_PLAN)),
}

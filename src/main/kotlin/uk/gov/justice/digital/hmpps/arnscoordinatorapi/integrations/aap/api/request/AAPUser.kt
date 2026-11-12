package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails

data class AAPUser(
  val id: String,
  val name: String,
) {
  companion object {
    fun from(userDetails: UserDetails) = AAPUser(id = userDetails.id, name = userDetails.name)
  }
}

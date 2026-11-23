package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.hook.formconfig.FormConfig

data class UpdateOasysDataMapping(
  val formConfig: FormConfig,
) : Hook

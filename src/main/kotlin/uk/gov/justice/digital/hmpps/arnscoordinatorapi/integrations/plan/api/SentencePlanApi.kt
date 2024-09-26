package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.response.CreatePlanResponse
import java.util.*

@Component
class SentencePlanApi(
  val sentencePlanApiWebClient: WebClient,
  val apiProperties: SentencePlanApiProperties,
) {

  fun createPlan(): CreatePlanResponse {
    return CreatePlanResponse(
      sentencePlanVersion = 0,
      sentencePlanId = UUID.randomUUID(),
    )
  }
}

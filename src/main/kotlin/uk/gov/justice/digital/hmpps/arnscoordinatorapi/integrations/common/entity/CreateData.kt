package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.assessment.api.request.CreateAssessmentData
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.api.request.CreatePlanData

data class CreateData(
  val plan: CreatePlanData,
  val assessment: CreateAssessmentData,
)

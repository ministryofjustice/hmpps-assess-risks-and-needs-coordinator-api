package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.UpdateCommandHelper
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.UpdateCommandHelper.createGoal
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.oasys.CreateOasysHelper
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.oasys.OasysHelper.lock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.oasys.OasysHelper.rollback
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.entity.GetEntityHelper.getEntitySentencePlan
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.entity.GetEntityVersionsHelper.getEntityVersions
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanState
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysGenericRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.time.LocalDate
import java.util.UUID
import kotlin.properties.Delegates
import kotlin.random.Random

@Tag("test")
@DisplayName("Rollback to wip API Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoordinatorApiRollBackToWipTest : IntegrationTestBase() {
  private lateinit var sentencePlanId: UUID
  private lateinit var assessmentId: UUID
  private var sentencePlanVersion by Delegates.notNull<Long>()
  private var assessmentVersion by Delegates.notNull<Long>()

  val crn = Random.nextInt(0, 100000).toString().padStart(5, '0')
  val oasysPk = Random.nextInt(0, 1_000_000_000).toString()
  val oasysUserDetails = OasysUserDetails(name = "Test")
  val oasysGenericRequest = OasysGenericRequest(oasysUserDetails)
  val today = LocalDate.now()

  @BeforeAll
  fun `create oasys and lock plan`() {
    val createOasysResponse = CreateOasysHelper.createOasys(webTestClient, crn, oasysPk)

    sentencePlanId = createOasysResponse?.sentencePlanId!!
    assessmentId = createOasysResponse.sanAssessmentId

    createGoal(aapTestClient, sentencePlanId, crn)!!

    val getEntityResponse = getEntitySentencePlan(webTestClient, sentencePlanId)

    assertThat(getEntityResponse?.planType).isEqualTo(PlanType.INITIAL)
    assertThat(getEntityResponse?.planComplete).isEqualTo(PlanState.INCOMPLETE)

    UpdateCommandHelper.postUpdateCommand(aapTestClient, assessmentId)

    var getEntityVersionsResponse = getEntityVersions(webTestClient, sentencePlanId)
    assessmentVersion = getEntityVersionsResponse?.allVersions[today]?.assessmentVersion?.version!!

    assertThat(getEntityVersionsResponse.allVersions[today]?.assessmentVersion?.status).isEqualTo("LOCKED_INCOMPLETE")

    lock(webTestClient, oasysPk, oasysGenericRequest)

    getEntityVersionsResponse = getEntityVersions(webTestClient, sentencePlanId)
    sentencePlanVersion = getEntityVersionsResponse?.allVersions?.get(today)?.planVersion?.version!!
    assessmentVersion = getEntityVersionsResponse.allVersions[today]?.assessmentVersion?.version!!

    assertThat(getEntityVersionsResponse.allVersions[today]?.planVersion?.status).isEqualTo("LOCKED")
  }

  @Test
  fun `rollback plan`() {
    val oasysRollbackRequest = OasysRollbackRequest(
      sanVersionNumber = assessmentVersion,
      sentencePlanVersionNumber = sentencePlanVersion,
      userDetails = oasysUserDetails,
    )
    rollback(webTestClient, oasysPk, oasysRollbackRequest)

    val getEntityResponse = getEntitySentencePlan(webTestClient, sentencePlanId)
    assertThat(getEntityResponse?.planComplete).isEqualTo(PlanState.INCOMPLETE)
  }
}


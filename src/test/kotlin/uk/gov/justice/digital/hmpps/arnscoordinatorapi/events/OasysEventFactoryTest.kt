package uk.gov.justice.digital.hmpps.arnscoordinatorapi.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.Clock
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.time.LocalDateTime
import java.util.UUID

class OasysEventFactoryTest {

  private val fixedNow = LocalDateTime.of(2026, 1, 1, 12, 0, 0)
  private val clock: Clock = mock()
  private val factory = OasysEventFactory(clock)

  private val planId = UUID.randomUUID()
  private val result = OasysVersionedEntityResponse(
    sentencePlanId = planId,
    sentencePlanVersion = 3L,
  )

  private val association = OasysAssociation(
    oasysAssessmentPk = "12345",
    entityType = EntityType.AAP_PLAN,
    entityUuid = planId,
    regionPrisonCode = "MDI",
    baseVersion = 2L,
  )

  @BeforeEach
  fun setup() {
    whenever(clock.now()).thenReturn(fixedNow)
  }

  @Test
  fun `createVersionEvent produces OASYS_VERSION_EVENT with CREATED`() {
    val request = OasysCreateRequest(
      oasysAssessmentPk = "12345",
      regionPrisonCode = "MDI",
      planType = PlanType.INITIAL,
      assessmentType = AssessmentType.SAN_SP,
      userDetails = OasysUserDetails(id = "1", name = "Test"),
    )

    val event = factory.createVersionEvent(request, result)

    assertThat(event.eventType).isEqualTo(EventType.OASYS_VERSION_EVENT)
    assertThat(event.entityType).isEqualTo("AAP_PLAN")
    assertThat(event.entityUuid).isEqualTo(planId)
    assertThat(event.occurredAt).isEqualTo(fixedNow)
    val payload = event.message as VersionPayload
    assertThat(payload.oasysEvent).isEqualTo(OasysEvent.CREATED)
    assertThat(payload.version).isEqualTo(3L)
    assertThat(payload.deleted).isFalse()
    assertThat(payload.association.oasysAssessmentPk).isEqualTo("12345")
    assertThat(payload.association.regionPrisonCode).isEqualTo("MDI")
  }

  @Test
  fun `signVersionEvent produces OASYS_VERSION_EVENT with SELF_SIGNED`() {
    val event = factory.signVersionEvent(association, result)

    val payload = event.message as VersionPayload
    assertThat(payload.oasysEvent).isEqualTo(OasysEvent.SELF_SIGNED)
    assertThat(payload.association.oasysAssessmentPk).isEqualTo("12345")
    assertThat(payload.association.baseVersion).isEqualTo(2L)
  }

  @Test
  fun `lockVersionEvent produces OASYS_VERSION_EVENT with LOCKED`() {
    val payload = (factory.lockVersionEvent(association, result).message) as VersionPayload
    assertThat(payload.oasysEvent).isEqualTo(OasysEvent.LOCKED)
  }

  @Test
  fun `rollbackVersionEvent produces OASYS_VERSION_EVENT with ROLLED_BACK`() {
    val payload = (factory.rollbackVersionEvent(association, result).message) as VersionPayload
    assertThat(payload.oasysEvent).isEqualTo(OasysEvent.ROLLED_BACK)
  }

  @ParameterizedTest
  @EnumSource(CounterSignOutcome::class)
  fun `counterSignVersionEvent maps CounterSignOutcome to correct OasysEvent`(outcome: CounterSignOutcome) {
    val request = OasysCounterSignRequest(
      sanVersionNumber = 1,
      sentencePlanVersionNumber = 1,
      outcome = outcome,
      userDetails = OasysUserDetails(id = "1", name = "Test"),
    )
    val payload = (factory.counterSignVersionEvent(association, request, result).message) as VersionPayload
    val expectedEvent = when (outcome) {
      CounterSignOutcome.COUNTERSIGNED -> OasysEvent.COUNTERSIGNED
      CounterSignOutcome.AWAITING_DOUBLE_COUNTERSIGN -> OasysEvent.AWAITING_DOUBLE_COUNTERSIGN
      CounterSignOutcome.DOUBLE_COUNTERSIGNED -> OasysEvent.DOUBLE_COUNTERSIGNED
      CounterSignOutcome.REJECTED -> OasysEvent.REJECTED
    }
    assertThat(payload.oasysEvent).isEqualTo(expectedEvent)
  }

  @Test
  fun `softDeleteEvent produces OASYS_DELETE_FLAG_UPDATE_EVENT with deleted=true`() {
    val event = factory.softDeleteEvent(association, versionTo = 5L)

    assertThat(event.eventType).isEqualTo(EventType.OASYS_DELETE_FLAG_UPDATE_EVENT)
    assertThat(event.entityUuid).isEqualTo(planId)
    val payload = event.message as DeleteFlagUpdatePayload
    assertThat(payload.deleted).isTrue()
    assertThat(payload.versionFrom).isEqualTo(2L)
    assertThat(payload.versionTo).isEqualTo(5L)
  }

  @Test
  fun `undeleteEvent produces OASYS_DELETE_FLAG_UPDATE_EVENT with deleted=false`() {
    val event = factory.undeleteEvent(association, versionTo = null)

    val payload = event.message as DeleteFlagUpdatePayload
    assertThat(payload.deleted).isFalse()
    assertThat(payload.versionTo).isNull()
  }
}

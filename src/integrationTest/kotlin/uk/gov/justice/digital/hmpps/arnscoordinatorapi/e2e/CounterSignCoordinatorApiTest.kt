package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.config.CounterSignOutcome
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.associations.GetAssociationsHelper.getAssociations
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.entity.GetEntityVersionsHelper.getEntityVersions
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.SignType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCounterSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysRollbackRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysSignRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import java.time.LocalDate
import java.util.UUID
import kotlin.properties.Delegates

@Tag("test")
@DisplayName("Coordinator counter-sign API Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CounterSignCoordinatorApiTest : IntegrationTestBase() {
  private lateinit var sentencePlanId: UUID
  private lateinit var assessmentId: UUID
  private var sentencePlanVersion by Delegates.notNull<Long>()
  private var assessmentVersion by Delegates.notNull<Long>()
  val oasysPk = "3173002"
  val today: LocalDate? = LocalDate.now()

  @BeforeAll
  fun `get association and set version`() {
    val getAssociationsResponse = getAssociations(webTestClient, oasysPk)
    assertThat(getAssociationsResponse).isNotNull

    sentencePlanId = getAssociationsResponse?.sentencePlanId!!
    assessmentId = getAssociationsResponse.sanAssessmentId

    val entityVersion = getEntityVersions(webTestClient, assessmentId)

    assertThat(entityVersion?.allVersions).isNotNull

    val planLastUpdatedDate = entityVersion?.allVersions?.entries?.lastOrNull { (_, versionValue) ->
      versionValue?.planVersion != null
    }?.key

    assertThat(entityVersion?.allVersions?.get(planLastUpdatedDate)?.planVersion?.entityType).isEqualTo(
      EntityType.AAP_PLAN,
    )

    val assessmentUpdatedDate = entityVersion?.allVersions?.entries?.lastOrNull { (_, versionValue) ->
      versionValue?.assessmentVersion != null
    }?.key

    assertThat(entityVersion?.allVersions?.get(assessmentUpdatedDate)?.assessmentVersion?.entityType).isEqualTo(
      EntityType.AAP_SAN,
    )

    sentencePlanVersion = entityVersion?.allVersions?.get(planLastUpdatedDate)?.planVersion?.version!!
    assessmentVersion = entityVersion.allVersions[assessmentUpdatedDate]?.assessmentVersion?.version!!
  }

  @Test
  @Order(1)
  fun `rollback plan`() {
    val oasysRollbackRequest = OasysRollbackRequest(
      sanVersionNumber = assessmentVersion,
      sentencePlanVersionNumber = sentencePlanVersion,
      userDetails = OasysUserDetails(name = "Burley Zieme", id = oasysPk),
    )
    webTestClient.post().uri("/oasys/$oasysPk/rollback")
      .bodyValue(oasysRollbackRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody

    val getEntityVersionsResponse = getEntityVersions(webTestClient, sentencePlanId)
    assertThat(getEntityVersionsResponse?.allVersions?.get(today)?.planVersion?.status).isEqualTo("ROLLED_BACK")

    assessmentVersion = getEntityVersionsResponse?.allVersions?.get(today)?.assessmentVersion?.version!!
    sentencePlanVersion = getEntityVersionsResponse.allVersions[today]?.planVersion?.version!!
  }

  @Test
  @Order(2)
  fun `sign plan`() {
    val oasysSignRequest = OasysSignRequest(
      signType = SignType.COUNTERSIGN,
      userDetails = OasysUserDetails(name = "Burley Zieme", id = oasysPk),
    )
    webTestClient.post().uri("/oasys/$oasysPk/sign")
      .bodyValue(oasysSignRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody

    val getEntityVersionsResponse = getEntityVersions(webTestClient, sentencePlanId)
    assertThat(getEntityVersionsResponse?.allVersions?.get(today)?.planVersion?.status).isEqualTo("AWAITING_COUNTERSIGN")
  }

  @Test
  @Order(3)
  fun `counter-sign plan`() {
    val oasysCounterSignRequest = OasysCounterSignRequest(
      sanVersionNumber = assessmentVersion,
      sentencePlanVersionNumber = sentencePlanVersion,
      outcome = CounterSignOutcome.COUNTERSIGNED,
      userDetails = OasysUserDetails(name = "Burley Zieme", id = oasysPk),
    )
    webTestClient.post().uri("/oasys/$oasysPk/counter-sign")
      .bodyValue(oasysCounterSignRequest)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult().responseBody

    val getEntityVersionsResponse = getEntityVersions(webTestClient, sentencePlanId)
    assertThat(getEntityVersionsResponse?.countersignedVersions?.get(today)?.planVersion?.status).isEqualTo("COUNTERSIGNED")
  }
}

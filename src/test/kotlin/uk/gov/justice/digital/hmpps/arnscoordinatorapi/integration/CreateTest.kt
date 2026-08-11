package uk.gov.justice.digital.hmpps.arnscoordinatorapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.web.reactive.server.expectBody
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.plan.entity.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociation
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.associations.repository.OasysAssociationRepository
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.oasys.entity.OasysUserDetails
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.net.URI
import java.util.UUID

class CreateTest : IntegrationTestBase() {

  @Autowired
  lateinit var oasysAssociationRepository: OasysAssociationRepository

  @Value("\${hmpps.sqs.localstackUrl}")
  lateinit var localStackUrl: String

  val queueUrl get() = "$localStackUrl/000000000000/coordinator-queue"

  val sqsClient: SqsClient by lazy {
    SqsClient.builder()
      .endpointOverride(URI.create(localStackUrl))
      .region(Region.EU_WEST_2)
      .credentialsProvider(
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create("test", "test"),
        ),
      )
      .build()
  }

  @BeforeEach
  fun setUp() {
    stubGrantToken()
    stubAssessmentsCreate()
    stubAAPCreateAssessment()
    stubAAPUpdateFlags()
    stubAssessmentsClone()
  }

  @BeforeEach
  fun clearQueue() {
    while (true) {
      val messages = sqsClient.receiveMessage {
        it.queueUrl(queueUrl)
          .maxNumberOfMessages(10)
          .waitTimeSeconds(0)
      }.messages()

      if (messages.isEmpty()) break

      messages.forEach { message ->
        sqsClient.deleteMessage {
          it.queueUrl(queueUrl).receiptHandle(message.receiptHandle())
        }
      }
    }
  }

  @Test
  fun `it successfully creates a new SP and SAN with no previous oasys PK`() {
    val oasysAssessmentPk = getRandomOasysPk()
    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isCreated
    val associations = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk)
    val aapPlanAssociation = associations.firstOrNull { it.entityType == EntityType.AAP_PLAN }
    val sanAssociation = associations.firstOrNull { it.entityType == EntityType.ASSESSMENT }
    assertThat(aapPlanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(sanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(aapPlanAssociation?.entityUuid).isEqualTo(UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"))
    assertThat(sanAssociation?.entityUuid).isEqualTo(UUID.fromString("90a71d16-fecd-4e1a-85b9-98178bf0f8d0"))
  }

  @Test
  fun `it publishes an OASYS_VERSION_EVENT to SQS when create succeeds`() {
    val oasysAssessmentPk = getRandomOasysPk()

    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          regionPrisonCode = "MDI",
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isCreated

    val messages = sqsClient.receiveMessage {
      it.queueUrl(queueUrl)
        .messageAttributeNames("All")
        .maxNumberOfMessages(1)
        .waitTimeSeconds(2)
    }.messages()

    assertThat(messages).hasSize(1)
    assertThat(messages.first().messageAttributes()["eventType"]?.stringValue()).isEqualTo("OASYS_VERSION_EVENT")
    assertThat(messages.first().body()).contains(oasysAssessmentPk)
    assertThat(messages.first().body()).contains("\"entityType\":\"AAP_PLAN\"")
  }

  @Test
  fun `it returns a 500 status where a call to the downstream AAP service returns 500`() {
    stubAAPCreateAssessment(500)
    val oasysAssessmentPk = getRandomOasysPk()
    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isEqualTo(500)

    val associations = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk)
    assertThat(associations).isEmpty()
  }

  @Test
  fun `it does not publish an event when create fails`() {
    stubAAPCreateAssessment(500)
    val oasysAssessmentPk = getRandomOasysPk()

    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isEqualTo(500)

    val messages = sqsClient.receiveMessage {
      it.queueUrl(queueUrl)
        .maxNumberOfMessages(1)
        .waitTimeSeconds(1)
        .visibilityTimeout(1)
        .messageAttributeNames("All")
    }.messages()

    assertThat(messages).isEmpty()
  }

  @Test
  fun `it returns a 400 status where no oasysAssessmentPk provided`() {
    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_WRITE")))
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `it successfully links existing SP and SAN when previous PKs are supplied`() {
    val previousOasysPk = getRandomOasysPk()
    val oasysAssessmentPk = getRandomOasysPk()

    oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociation(
          oasysAssessmentPk = previousOasysPk,
          entityType = EntityType.AAP_PLAN,
          entityUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"),
        ),
        OasysAssociation(
          oasysAssessmentPk = previousOasysPk,
          entityType = EntityType.ASSESSMENT,
          entityUuid = UUID.fromString("90a71d16-fecd-4e1a-85b9-98178bf0f8d0"),
        ),
      ),
    )

    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          previousOasysSanPk = previousOasysPk,
          previousOasysSpPk = previousOasysPk,
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isCreated
    val associations = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk)
    val sentencePlanAssociation = associations.firstOrNull { it.entityType == EntityType.AAP_PLAN }
    val sanAssociation = associations.firstOrNull { it.entityType == EntityType.ASSESSMENT }
    assertThat(sentencePlanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(sanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(sentencePlanAssociation?.entityUuid).isEqualTo(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"))
    assertThat(sanAssociation?.entityUuid).isEqualTo(UUID.fromString("90a71d16-fecd-4e1a-85b9-98178bf0f8d0"))
    verifyAAPUpdateFlags(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"), listOf("SAN_BETA"))
  }

  @Test
  fun `it clears AAP flags when an SP assessment links an existing sentence plan`() {
    val previousOasysPk = getRandomOasysPk()
    val oasysAssessmentPk = getRandomOasysPk()
    val sentencePlanUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")

    oasysAssociationRepository.save(
      OasysAssociation(
        oasysAssessmentPk = previousOasysPk,
        entityType = EntityType.AAP_PLAN,
        entityUuid = sentencePlanUuid,
      ),
    )

    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          previousOasysSpPk = previousOasysPk,
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isCreated

    verifyAAPUpdateFlags(sentencePlanUuid, emptyList())
  }

  @Test
  fun `it returns a conflict status where an association already exists with the oasys PK`() {
    val oasysAssessmentPk = getRandomOasysPk()

    oasysAssociationRepository.save(
      OasysAssociation(
        oasysAssessmentPk = oasysAssessmentPk,
        entityUuid = UUID.randomUUID(),
        entityType = EntityType.ASSESSMENT,
      ),
    )

    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isEqualTo(409)
  }

  @Test
  fun `it returns a 404 not found when a previous oasys PK is supplied that does not have an association`() {
    val previousOasysPk = getRandomOasysPk()
    val oasysAssessmentPk = getRandomOasysPk()

    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          previousOasysSanPk = previousOasysPk,
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `it returns 400 when validation errors occur in the body`() {
    val invalidPk = "012345678901234A"

    val response = webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          previousOasysSanPk = invalidPk,
          oasysAssessmentPk = invalidPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
      .expectBody<ErrorResponse>()
      .returnResult()

    assertThat(response.responseBody?.developerMessage).contains("previousOasysSanPk - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("previousOasysSanPk - size must be between 1 and 15")
    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPk - Must only contain numeric characters")
    assertThat(response.responseBody?.developerMessage).contains("oasysAssessmentPk - size must be between 1 and 15")
  }
}

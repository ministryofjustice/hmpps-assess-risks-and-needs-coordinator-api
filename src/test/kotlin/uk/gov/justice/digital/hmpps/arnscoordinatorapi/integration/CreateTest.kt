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
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.AssessmentType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.OasysCreateRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.request.dto.OasysUserDetails
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.controller.response.OasysVersionedEntityResponse
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.EntityType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.model.plan.PlanType
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.entity.OasysAssociationEntity
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.persistence.repository.OasysAssociationRepository
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
    stubAAPCreateAssessment(201, "SENTENCE_PLAN", UUID.randomUUID())
    stubAAPCreateAssessment(201, "STRENGTHS_AND_NEEDS", UUID.randomUUID())
    stubAAPUpdateFlags()
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
    val planUuid = UUID.randomUUID()
    val sanUuid = UUID.randomUUID()

    stubAAPCreateAssessment(201, "SENTENCE_PLAN", planUuid)
    stubAAPCreateAssessment(201, "STRENGTHS_AND_NEEDS", sanUuid)

    val oasysAssessmentPk = getRandomOasysPk()
    val response = webTestClient.post().uri("/oasys/create")
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
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult()
      .responseBody

    assertThat(response?.sentencePlanId).isEqualTo(planUuid)
    assertThat(response?.sanAssessmentId).isEqualTo(sanUuid)
    assertThat(response?.sentencePlanVersion).isPositive()
    assertThat(response?.sanAssessmentVersion).isPositive()
    assertThat(aap.commandsSent("CreateAssessmentCommand").map { it.path("assessmentType").asText() })
      .containsExactly("SENTENCE_PLAN", "STRENGTHS_AND_NEEDS")

    val associations = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk)
    val aapPlanAssociation = associations.firstOrNull { it.entityType == EntityType.AAP_PLAN }
    val sanAssociation = associations.firstOrNull { it.entityType == EntityType.AAP_SAN }
    assertThat(aapPlanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(sanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(aapPlanAssociation?.entityUuid).isEqualTo(planUuid)
    assertThat(sanAssociation?.entityUuid).isEqualTo(sanUuid)
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
    stubAAPCreateAssessment(500, "SENTENCE_PLAN", UUID.randomUUID())
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
    stubAAPCreateAssessment(500, "SENTENCE_PLAN", UUID.randomUUID())
    stubAAPCreateAssessment(500, "STRENGTHS_AND_NEEDS", UUID.randomUUID())
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
    val planUuid = UUID.randomUUID()
    val sanUuid = UUID.randomUUID()

    stubAAPCreateAssessment(201, "SENTENCE_PLAN", planUuid)
    stubAAPCreateAssessment(201, "STRENGTHS_AND_NEEDS", sanUuid)
    stubAAPUpdateFlags()

    val previousOasysPk = getRandomOasysPk()
    val oasysAssessmentPk = getRandomOasysPk()

    val (planAssociation, sanAssociation) = oasysAssociationRepository.saveAll(
      listOf(
        OasysAssociationEntity(
          oasysAssessmentPk = previousOasysPk,
          entityType = EntityType.AAP_PLAN,
          entityUuid = planUuid,
        ),
        OasysAssociationEntity(
          oasysAssessmentPk = previousOasysPk,
          entityType = EntityType.AAP_SAN,
          entityUuid = sanUuid,
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
    val persistedSentencePlanAssociation = associations.firstOrNull { it.entityType == EntityType.AAP_PLAN }
    val persistedSanAssociation = associations.firstOrNull { it.entityType == EntityType.AAP_SAN }
    assertThat(persistedSentencePlanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(persistedSanAssociation?.oasysAssessmentPk).isEqualTo(oasysAssessmentPk)
    assertThat(persistedSentencePlanAssociation?.entityUuid).isEqualTo(planAssociation.entityUuid)
    assertThat(persistedSanAssociation?.entityUuid).isEqualTo(sanAssociation.entityUuid)
    verifyAAPUpdateFlags(planAssociation.entityUuid, listOf("SAN_BETA"))
  }

  @Test
  fun `it clears AAP flags when an SP assessment links an existing sentence plan`() {
    val previousOasysPk = getRandomOasysPk()
    val oasysAssessmentPk = getRandomOasysPk()
    val sentencePlanUuid = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")

    oasysAssociationRepository.save(
      OasysAssociationEntity(
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
      OasysAssociationEntity(
        oasysAssessmentPk = oasysAssessmentPk,
        entityUuid = UUID.randomUUID(),
        entityType = EntityType.AAP_SAN,
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

  @Test
  fun `it only creates a sentence plan for an SP assessment`() {
    val planUuid = UUID.randomUUID()
    stubAAPCreateAssessment(201, "SENTENCE_PLAN", planUuid)
    val oasysAssessmentPk = getRandomOasysPk()

    val response = webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult()
      .responseBody

    assertThat(response?.sentencePlanId).isEqualTo(planUuid)
    assertThat(response?.sanAssessmentId).isEqualTo(UUID(0, 0))
    assertThat(aap.commandsSent("CreateAssessmentCommand").map { it.path("assessmentType").asText() }).containsExactly("SENTENCE_PLAN")
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk).map { it.entityType }).containsExactly(EntityType.AAP_PLAN)
  }

  @Test
  fun `it links an existing SP and creates a new SAN`() {
    val existingPlanUuid = UUID.randomUUID()
    val newSanUuid = UUID.randomUUID()
    stubAAPCreateAssessment(201, "STRENGTHS_AND_NEEDS", newSanUuid)

    val previousOasysPk = getRandomOasysPk()
    val oasysAssessmentPk = getRandomOasysPk()
    oasysAssociationRepository.save(
      OasysAssociationEntity(oasysAssessmentPk = previousOasysPk, entityType = EntityType.AAP_PLAN, entityUuid = existingPlanUuid),
    )

    val response = webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          previousOasysSpPk = previousOasysPk,
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SAN_SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody<OasysVersionedEntityResponse>()
      .returnResult()
      .responseBody

    assertThat(response?.sentencePlanId).isEqualTo(existingPlanUuid)
    assertThat(response?.sanAssessmentId).isEqualTo(newSanUuid)

    val associations = oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk).associateBy { it.entityType }
    assertThat(associations[EntityType.AAP_PLAN]?.entityUuid).isEqualTo(existingPlanUuid)
    assertThat(associations[EntityType.AAP_PLAN]?.baseVersion).isEqualTo(response?.sentencePlanVersion)
    assertThat(associations[EntityType.AAP_SAN]?.entityUuid).isEqualTo(newSanUuid)
    verifyAAPUpdateFlags(existingPlanUuid, listOf("SAN_BETA"))
  }

  @Test
  fun `it resets the linked sentence plan for a new period of supervision`() {
    val existingPlanUuid = UUID.randomUUID()
    val previousOasysPk = getRandomOasysPk()
    val oasysAssessmentPk = getRandomOasysPk()
    oasysAssociationRepository.save(
      OasysAssociationEntity(oasysAssessmentPk = previousOasysPk, entityType = EntityType.AAP_PLAN, entityUuid = existingPlanUuid),
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
          newPeriodOfSupervision = "Y",
        ),
      )
      .exchange()
      .expectStatus().isCreated

    assertThat(aap.commandsSent("UpdateAssessmentPropertiesCommand").map { it.path("assessmentUuid").asText() })
      .containsExactly(existingPlanUuid.toString())
    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk).single().entityUuid).isEqualTo(existingPlanUuid)
  }

  @Test
  fun `it creates a new sentence plan for a new period of supervision when there is no previous SP`() {
    val planUuid = UUID.randomUUID()
    stubAAPCreateAssessment(201, "SENTENCE_PLAN", planUuid)
    val oasysAssessmentPk = getRandomOasysPk()

    webTestClient.post().uri("/oasys/create")
      .headers(setAuthorisation(roles = listOf("ROLE_STRENGTHS_AND_NEEDS_OASYS")))
      .bodyValue(
        OasysCreateRequest(
          oasysAssessmentPk = oasysAssessmentPk,
          planType = PlanType.INITIAL,
          assessmentType = AssessmentType.SP,
          userDetails = OasysUserDetails(id = "1", name = "Test Name"),
          newPeriodOfSupervision = "Y",
        ),
      )
      .exchange()
      .expectStatus().isCreated

    assertThat(oasysAssociationRepository.findAllByOasysAssessmentPk(oasysAssessmentPk).single().entityUuid).isEqualTo(planUuid)
  }
}

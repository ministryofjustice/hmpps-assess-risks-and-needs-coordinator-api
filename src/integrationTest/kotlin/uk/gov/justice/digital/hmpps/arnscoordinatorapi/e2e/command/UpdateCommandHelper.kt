package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request.AddCollectionItemCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request.CreateCollectionCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.response.CommandSuccessCommandResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.response.CreateCollectionItemResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.response.CreateCollectionResult
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.PropertyValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.MultiValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.response.query.SingleValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import java.util.UUID
import kotlin.random.Random

object UpdateCommandHelper {
  fun postUpdateCommand(aapTestClient: WebTestClient, assessmentUuid: UUID): CommandSuccessCommandResult? {
    val commandRequest = UpdateAssessmentAnswersCommand(
      assessmentUuid = assessmentUuid,
      user = UserDetails("test-user", "Test User"),
      added = mapOf(
        "thinking_behaviours_attitudes_risk_sexual_harm" to PropertyValue(type = "Single", value = "YES"),
      ),
      removed = listOf(),
    )

    val payload = CommandsRequest(commands = listOf(commandRequest))

    val getResponse = aapTestClient.post().uri("/command")
      .bodyValue(payload)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<CommandSuccessCommandResult>()
      .returnResult().responseBody

    return getResponse
  }

  fun createGoal(aapTestClient: WebTestClient, assessmentUuid: UUID): UUID? {
    val crn = Random.nextInt(0, 100_000).toString().padStart(5, '0')

    val collectionRequest = CreateCollectionCommand(
      assessmentUuid = assessmentUuid,
      user = UserDetails("test-user", "Test User"),
      name = "GOALS",
      identifiers = mapOf("CRN" to crn),
      flags = listOf("SAN_BETA"),
    )

    val goalCollection = CommandsRequest(commands = listOf(collectionRequest))

    val getCollectionResponse = aapTestClient.post().uri("/command")
      .bodyValue(goalCollection)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<CreateCollectionResult>()
      .returnResult().responseBody

    val itemRequest = AddCollectionItemCommand(
      name = "GOALS",
      assessmentUuid = assessmentUuid,
      collectionUuid = getCollectionResponse?.commands?.firstOrNull()?.result?.collectionUuid!!,
      user = UserDetails("test-user", "Test User"),
      identifiers = mapOf("CRN" to crn),
      flags = listOf("SAN_BETA"),
      properties = mapOf(
        "status" to SingleValue("ACTIVE"),
        "status_date" to SingleValue("2026-02-23T17:30:11.179Z"),
      ),
      answers = mapOf(
        "title" to SingleValue("I will work towards finding accommodation, so that I am no longer homeless"),
        "target_date" to SingleValue("2026-05-23T16:30:11.151Z"),
        "area_of_need" to SingleValue("accommodation"),
        "related_areas_of_need" to MultiValue(listOf("alcohol-use")),
      ),
    )

    val goal = CommandsRequest(commands = listOf(itemRequest))

    val getGoalResponse = aapTestClient.post().uri("/command")
      .bodyValue(goal)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<CreateCollectionItemResult>()
      .returnResult().responseBody

    return getGoalResponse?.commands?.get(0)?.result?.collectionItemUuid
  }
}

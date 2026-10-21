package uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request.CommandsRequest
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.request.UpdateAssessmentAnswersCommand
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.e2e.command.response.CommandsResponse

import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.aap.api.request.command.PropertyValue
import uk.gov.justice.digital.hmpps.arnscoordinatorapi.integrations.common.entity.UserDetails
import java.util.UUID

object UpdateCommandHelper {
  fun postUpdateCommand(aapTestClient: WebTestClient, assessmentUuid: UUID): CommandsResponse? {
    val commandRequest = UpdateAssessmentAnswersCommand(
      assessmentUuid = assessmentUuid,
      user = UserDetails("test-user", "Test User"),
      added = mapOf(
        "thinking_behaviours_attitudes_risk_sexual_harm" to PropertyValue(type = "Single", value = "YES"),
      ),
      removed = listOf()
    )

    val payload = CommandsRequest(commands = listOf(commandRequest))

    val getResponse = aapTestClient.post().uri("/command")
      .bodyValue(payload)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
      .expectBody<CommandsResponse>()
      .returnResult().responseBody

    return getResponse
  }
}

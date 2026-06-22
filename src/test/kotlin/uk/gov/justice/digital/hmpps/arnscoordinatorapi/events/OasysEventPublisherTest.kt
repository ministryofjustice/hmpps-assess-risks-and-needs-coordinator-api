package uk.gov.justice.digital.hmpps.arnscoordinatorapi.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

class OasysEventPublisherTest {

  private val hmppsQueueService = mock<HmppsQueueService>()
  private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
  private val sqsClient = mock<SqsAsyncClient>()
  private val queue = mock<HmppsQueue>()

  @Test
  fun `publish sends serialized event to coordinator queue with event type attribute`() {
    whenever(hmppsQueueService.findByQueueId("coordinator")).thenReturn(queue)
    whenever(queue.queueUrl).thenReturn("http://localstack:4566/queue/coordinator-queue")
    whenever(queue.sqsClient).thenReturn(sqsClient)
    whenever(sqsClient.sendMessage(org.mockito.kotlin.any<SendMessageRequest>()))
      .thenReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().messageId("123").build()))

    val publisher = OasysEventPublisher(hmppsQueueService, objectMapper)

    val event = CoordinatorEvent(
      eventType = EventType.OASYS_VERSION_EVENT,
      entityType = "AAP_PLAN",
      entityUuid = UUID.fromString("5fa85f64-5717-4562-b3fc-2c963f66afa6"),
      occurredAt = LocalDateTime.of(2026, 6, 12, 10, 0, 0),
      message = VersionPayload(
        version = 7,
        oasysEvent = OasysEvent.CREATED,
        incrementedAt = LocalDateTime.of(2026, 6, 12, 10, 0, 0),
        deleted = false,
        association = AssociationPayload(
          oasysAssessmentPk = "12345",
          regionPrisonCode = "MDI",
          baseVersion = 1L,
        ),
      ),
    )

    publisher.publish(event)

    val captor = argumentCaptor<SendMessageRequest>()
    verify(sqsClient).sendMessage(captor.capture())

    val request = captor.firstValue
    assertThat(request.queueUrl()).isEqualTo("http://localstack:4566/queue/coordinator-queue")
    assertThat(request.messageBody()).isEqualTo(objectMapper.writeValueAsString(event))

    assertThat(request.messageAttributes()).containsKey("eventType")
    assertThat(request.messageAttributes()["eventType"]?.stringValue()).isEqualTo("OASYS_VERSION_EVENT")
  }
}

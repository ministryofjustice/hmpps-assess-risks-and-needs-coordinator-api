package uk.gov.justice.digital.hmpps.arnscoordinatorapi.events

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.eventTypeMessageAttributes

fun interface OasysEventPublisher {
  fun publish(event: CoordinatorEvent)
}

@Service
@ConditionalOnProperty(name = ["app.events.enabled"], havingValue = "true")
class SqsOasysEventPublisher(
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) : OasysEventPublisher {
  companion object {
    private val log = LoggerFactory.getLogger(SqsOasysEventPublisher::class.java)
    private const val QUEUE_ID = "coordinator"
  }

  private val queue = hmppsQueueService.findByQueueId(QUEUE_ID)
    ?: throw MissingQueueException("Could not find queue $QUEUE_ID")

  override fun publish(event: CoordinatorEvent) {
    val message = objectMapper.writeValueAsString(event)

    try {
      queue.sqsClient.sendMessage(
        SendMessageRequest.builder()
          .queueUrl(queue.queueUrl)
          .messageBody(message)
          .eventTypeMessageAttributes(event.eventType.name)
          .build(),
      ).get()

      log.info("Published OASys event type={}", event.eventType)
    } catch (ex: Exception) {
      log.error("Failed to publish OASys event type={}", event.eventType, ex)
    }
  }
}

@Service
@ConditionalOnProperty(name = ["app.events.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpOasysEventPublisher : OasysEventPublisher {
  companion object {
    private val log = LoggerFactory.getLogger(NoOpOasysEventPublisher::class.java)
  }

  override fun publish(event: CoordinatorEvent) {
    log.debug("Event publishing disabled, skipping event type={}", event.eventType)
  }
}

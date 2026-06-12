package uk.gov.justice.digital.hmpps.arnscoordinatorapi.events

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.eventTypeMessageAttributes
import com.fasterxml.jackson.databind.ObjectMapper

@Service
class OasysEventPublisher(
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  companion object {
    private val log = LoggerFactory.getLogger(OasysEventPublisher::class.java)
    private const val QUEUE_ID = "coordinator" // TODO: make it not fall over if the queue is not found?
  }

  private val queue = hmppsQueueService.findByQueueId(QUEUE_ID)
    ?: throw MissingQueueException("Could not find queue $QUEUE_ID")

  fun publish(event: CoordinatorEvent) {
    val message = objectMapper.writeValueAsString(event)

    queue.sqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(queue.queueUrl)
        .messageBody(message)
        .eventTypeMessageAttributes(event.eventType.name)
        .build()
        .also { log.info("Published OASys event type={}", event.eventType) },
    ).get()
  }
}
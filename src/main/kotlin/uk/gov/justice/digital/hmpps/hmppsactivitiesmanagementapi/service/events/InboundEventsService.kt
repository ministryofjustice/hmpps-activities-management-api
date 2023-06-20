package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.ActivitiesChangedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.InterestingEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReceivedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReleasedEventHandler

@Service
@Transactional
class InboundEventsService(
  private val releasedEventHandler: OffenderReleasedEventHandler,
  private val receivedEventHandler: OffenderReceivedEventHandler,
  private val interestingEventHandler: InterestingEventHandler,
  private val activitiesChangedEventHandler: ActivitiesChangedEventHandler,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun process(event: InboundEvent) {
    when (event) {
      is ActivitiesChangedEvent -> activitiesChangedEventHandler.handle(event)
        .onFailure { interestingEventHandler.handle(event) }

      is OffenderReceivedEvent -> receivedEventHandler.handle(event).onFailure { interestingEventHandler.handle(event) }
      is OffenderReleasedEvent -> releasedEventHandler.handle(event).onFailure { interestingEventHandler.handle(event) }
      is EventOfInterest -> interestingEventHandler.handle(event)
      else -> log.warn("Unsupported event ${event.javaClass.name}")
    }
  }
}

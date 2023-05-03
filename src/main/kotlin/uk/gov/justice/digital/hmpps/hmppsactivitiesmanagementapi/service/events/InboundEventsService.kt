package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.InterestingEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReceivedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReleasedEventHandler

@Service
class InboundEventsService(
  private val releasedEventHandler: OffenderReleasedEventHandler,
  private val receivedEventHandler: OffenderReceivedEventHandler,
  private val interestingEventHandler: InterestingEventHandler,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun process(inboundEvent: InboundEvent) {
    when (inboundEvent) {
      is OffenderReceivedEvent -> {
        if (!receivedEventHandler.handle(inboundEvent)) {
          interestingEventHandler.handle(inboundEvent)
        }
      }
      is OffenderReleasedEvent -> {
        if (!releasedEventHandler.handle(inboundEvent)) {
          interestingEventHandler.handle(inboundEvent)
        }
      }
      is IncentivesInsertedEvent -> interestingEventHandler.handle(inboundEvent)
      is IncentivesUpdatedEvent -> interestingEventHandler.handle(inboundEvent)
      is IncentivesDeletedEvent -> interestingEventHandler.handle(inboundEvent)
      is CellMoveEvent -> interestingEventHandler.handle(inboundEvent)
      is NonAssociationsChangedEvent -> interestingEventHandler.handle(inboundEvent)
      else -> log.warn("Unsupported event ${inboundEvent.javaClass.name}")
    }
  }
}

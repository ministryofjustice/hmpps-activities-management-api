package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.ActivitiesChangedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.AppointmentChangedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.InterestingEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderMergedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReceivedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReleasedEventHandler

@Service
@Transactional
class InboundEventsService(
  private val releasedEventHandler: OffenderReleasedEventHandler,
  private val receivedEventHandler: OffenderReceivedEventHandler,
  private val interestingEventHandler: InterestingEventHandler,
  private val activitiesChangedEventHandler: ActivitiesChangedEventHandler,
  private val appointmentsChangedEventHandler: AppointmentChangedEventHandler,
  private val mergedEventHandler: OffenderMergedEventHandler,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun process(event: InboundEvent) {
    log.debug("Processing inbound event {}", event.eventType())

    when (event) {
      is ActivitiesChangedEvent -> activitiesChangedEventHandler.handle(event).run { interestingEventHandler.handle(event) }
      is AppointmentsChangedEvent -> appointmentsChangedEventHandler.handle(event).run { interestingEventHandler.handle(event) }
      is OffenderReceivedEvent -> receivedEventHandler.handle(event).run { interestingEventHandler.handle(event) }
      is OffenderReleasedEvent -> releasedEventHandler.handle(event).run { interestingEventHandler.handle(event) }
      is OffenderMergedEvent -> mergedEventHandler.handle(event).run { interestingEventHandler.handle(event) }
      is EventOfInterest -> interestingEventHandler.handle(event)
      else -> log.warn("Unsupported event ${event.javaClass.name}")
    }
  }
}

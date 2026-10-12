package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.ActivitiesChangedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.AppointmentChangedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.InterestingEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderMergedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.Outcome
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.PrisonerReceivedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.PrisonerReleasedEventHandler

@Service
@Transactional
class InboundEventsService(
  private val releasedEventHandler: PrisonerReleasedEventHandler,
  private val receivedEventHandler: PrisonerReceivedEventHandler,
  private val interestingEventHandler: InterestingEventHandler,
  private val activitiesChangedEventHandler: ActivitiesChangedEventHandler,
  private val appointmentsChangedEventHandler: AppointmentChangedEventHandler,
  private val mergedEventHandler: OffenderMergedEventHandler,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private fun requireSuccess(outcome: Outcome, event: InboundEvent) {
    if (!outcome.isSuccess()) {
      throw IllegalStateException("Failed to process inbound event ${event.eventType()}")
    }
  }

  fun process(event: InboundEvent) {
    log.debug("Processing inbound event {}", event.eventType())

    when (event) {
      is ActivitiesChangedEvent -> {
        requireSuccess(activitiesChangedEventHandler.handle(event), event)
        requireSuccess(interestingEventHandler.handle(event), event)
      }
      is AppointmentsChangedEvent -> {
        requireSuccess(appointmentsChangedEventHandler.handle(event), event)
        requireSuccess(interestingEventHandler.handle(event), event)
      }
      is PrisonerReceivedEvent -> {
        requireSuccess(receivedEventHandler.handle(event), event)
        requireSuccess(interestingEventHandler.handle(event), event)
      }
      is PrisonerReleasedEvent -> {
        val releaseOutcome = releasedEventHandler.handle(event)
        requireSuccess(interestingEventHandler.handle(event), event)
        requireSuccess(releaseOutcome, event)
      }
      is OffenderMergedEvent -> {
        requireSuccess(mergedEventHandler.handle(event), event)
        requireSuccess(interestingEventHandler.handle(event), event)
      }
      is EventOfInterest -> requireSuccess(interestingEventHandler.handle(event), event)
      else -> log.warn("Unsupported event ${event.javaClass.name}")
    }
  }
}

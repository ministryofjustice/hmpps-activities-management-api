package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReviewDescription
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.Action
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ActivitiesChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundReleaseEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderMergedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.LocalDateTime

/**
 * The interesting event handler is responsible for capturing potential events of interest that can affect prisoners at
 * a given prison in our service, so it can be surfaced to the end users of the service e.g. cell moves, release events.
 */
@Component
class InterestingEventHandler(
  private val rolloutPrisonService: RolloutPrisonService,
  private val allocationRepository: AllocationRepository,
  private val eventReviewRepository: EventReviewRepository,
  private val prisonerSearchApiAppWebClient: PrisonerSearchApiApplicationClient,
) : EventHandler<InboundEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: InboundEvent): Outcome {
    log.debug("Checking for interesting event: {}", event)

    if (!event.isMeaningful()) {
      return Outcome.success()
    }

    if (event is InboundReleaseEvent) return recordRelease(event)
    if (event is OffenderMergedEvent) return recordMerge(event)

    getPrisonerDetailsFor(event.prisonerNumber())?.let {
      it.prisonId?.let { agencyId ->
        if (rolloutPrisonService.isActivitiesRolledOutAt(agencyId)) {
          if (allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
              prisonCode = agencyId,
              prisonerNumber = event.prisonerNumber(),
              prisonerStatus = arrayOf(PrisonerStatus.ACTIVE, PrisonerStatus.PENDING),
            ).isNotEmpty()
          ) {
            val saved = eventReviewRepository.saveAndFlush(
              EventReview(
                eventTime = LocalDateTime.now(),
                eventType = event.eventType(),
                eventData = event.eventMessage(),
                prisonCode = agencyId,
                prisonerNumber = event.prisonerNumber(),
                bookingId = it.bookingId?.toInt(),
              ),
            )
            log.debug("Saved interesting event ID ${saved.eventReviewId} - ${event.eventType()} - for ${event.prisonerNumber()}")
            return Outcome.success()
          } else {
            log.info("${event.prisonerNumber()} has no active or pending allocations at $agencyId")
          }
        } else {
          log.debug("$agencyId is not a rolled out prison")
        }
      }
    }
    return Outcome.failed()
  }

  private fun recordRelease(releaseEvent: InboundReleaseEvent): Outcome {
    if (rolloutPrisonService.isActivitiesRolledOutAt(releaseEvent.prisonCode())) {
      getPrisonerDetailsFor(releaseEvent.prisonerNumber())?.let { prisoner ->
        val saved = eventReviewRepository.saveAndFlush(
          EventReview(
            eventTime = LocalDateTime.now(),
            eventType = releaseEvent.eventType(),
            eventData = releaseEvent.eventMessage(),
            // Release events use the prison code from the release event. The prisoner prison code could be different because they are released!
            prisonCode = releaseEvent.prisonCode(),
            prisonerNumber = releaseEvent.prisonerNumber(),
            bookingId = prisoner.bookingId?.toInt(),
            eventDescription = releaseEvent.getEventDesc(),
          ),
        )
        log.debug("Saved interesting event ID ${saved.eventReviewId} - ${releaseEvent.eventType()} - for ${releaseEvent.prisonerNumber()}")
        return Outcome.success()
      }
    } else {
      log.debug("${releaseEvent.prisonCode()} is not a rolled out prison")
    }

    return Outcome.success()
  }

  private fun recordMerge(mergedEvent: OffenderMergedEvent): Outcome {
    // Use the new prisoner number - the merged will have been actioned in prison API
    getPrisonerDetailsFor(mergedEvent.prisonerNumber())?.let {
      it.prisonId?.let { agencyId ->
        if (rolloutPrisonService.isActivitiesRolledOutAt(agencyId)) {
          val saved = eventReviewRepository.saveAndFlush(
            EventReview(
              eventTime = LocalDateTime.now(),
              eventType = mergedEvent.eventType(),
              eventData = mergedEvent.eventMessage(),
              prisonCode = agencyId,
              prisonerNumber = mergedEvent.prisonerNumber(),
              bookingId = it.bookingId?.toInt(),
              eventDescription = mergedEvent.getEventDesc(),
            ),
          )
          log.debug("Saved interesting event ID ${saved.eventReviewId} - ${mergedEvent.eventType()} - replaced ${mergedEvent.removedPrisonerNumber()} with ${mergedEvent.prisonerNumber()}")
        } else {
          log.debug("Ignoring offender merged event for ${mergedEvent.removedPrisonerNumber()} - prison $agencyId is not rolled out.")
        }
      }
    }

    return Outcome.success()
  }

  private fun InboundEvent.getEventDesc(): EventReviewDescription? = when (this) {
    is ActivitiesChangedEvent ->
      when (action()) {
        Action.END -> EventReviewDescription.ACTIVITY_ENDED
        Action.SUSPEND -> EventReviewDescription.ACTIVITY_SUSPENDED
        else -> null
      }
    is PrisonerReleasedEvent ->
      if (isPermanent()) {
        EventReviewDescription.PERMANENT_RELEASE
      } else if (isTemporary()) {
        EventReviewDescription.TEMPORARY_RELEASE
      } else {
        EventReviewDescription.RELEASED
      }
    else -> null
  }

  private fun getPrisonerDetailsFor(prisonerNumber: String) = prisonerSearchApiAppWebClient.findByPrisonerNumber(prisonerNumber)
}

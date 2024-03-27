package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.isActivitiesRolledOutAt
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ActivitiesChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AlertsUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.CellMoveEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundReleaseEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.IncentivesDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.IncentivesInsertedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.IncentivesUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.NonAssociationsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderMergedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerReceivedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerReleasedEvent
import java.time.LocalDateTime

/**
 * The interesting event handler is responsible for capturing potential events of interest that can affect prisoners at
 * a given prison in our service, so it can be surfaced to the end users of the service e.g. cell moves, release events.
 */
@Component
class InterestingEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val allocationRepository: AllocationRepository,
  private val eventReviewRepository: EventReviewRepository,
  private val prisonApiClient: PrisonApiApplicationClient,
) : EventHandler<InboundEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: InboundEvent): Outcome {
    log.debug("Checking for interesting event: {}", event)

    if (event is InboundReleaseEvent) return recordRelease(event)
    if (event is OffenderMergedEvent) return recordMerge(event)

    getPrisonerDetailsFor(event.prisonerNumber()).let { prisoner ->
      if (rolloutPrisonRepository.isActivitiesRolledOutAt(prisoner.agencyId!!)) {
        if (allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
            prisonCode = prisoner.agencyId,
            prisonerNumber = event.prisonerNumber(),
            prisonerStatus = arrayOf(PrisonerStatus.ACTIVE, PrisonerStatus.PENDING),
          ).isNotEmpty()
        ) {
          val saved = eventReviewRepository.saveAndFlush(
            EventReview(
              eventTime = LocalDateTime.now(),
              eventType = event.eventType(),
              eventData = event.getEventMessage(prisoner),
              prisonCode = prisoner.agencyId,
              prisonerNumber = event.prisonerNumber(),
              bookingId = prisoner.bookingId?.toInt(),
            ),
          )
          log.debug("Saved interesting event ID ${saved.eventReviewId} - ${event.eventType()} - for ${event.prisonerNumber()}")
          return Outcome.success()
        } else {
          log.info("${event.prisonerNumber()} has no active or pending allocations at ${prisoner.agencyId}")
        }
      } else {
        log.debug("${prisoner.agencyId} is not a rolled out prison")
      }
    }
    return Outcome.failed()
  }

  private fun recordRelease(releaseEvent: InboundReleaseEvent): Outcome {
    if (rolloutPrisonRepository.isActivitiesRolledOutAt(releaseEvent.prisonCode())) {
      getPrisonerDetailsFor(releaseEvent.prisonerNumber()).let { prisoner ->
        val saved = eventReviewRepository.saveAndFlush(
          EventReview(
            eventTime = LocalDateTime.now(),
            eventType = releaseEvent.eventType(),
            eventData = releaseEvent.getEventMessage(prisoner),
            // Release events use the prison code from the release event. The prisoner prison code could be different because they are released!
            prisonCode = releaseEvent.prisonCode(),
            prisonerNumber = releaseEvent.prisonerNumber(),
            bookingId = prisoner.bookingId?.toInt(),
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
    getPrisonerDetailsFor(mergedEvent.prisonerNumber()).let { prisoner ->
      if (rolloutPrisonRepository.isActivitiesRolledOutAt(prisoner.agencyId!!)) {
        val saved = eventReviewRepository.saveAndFlush(
          EventReview(
            eventTime = LocalDateTime.now(),
            eventType = mergedEvent.eventType(),
            eventData = mergedEvent.getEventMessage(prisoner),
            prisonCode = prisoner.agencyId,
            prisonerNumber = mergedEvent.prisonerNumber(),
            bookingId = prisoner.bookingId?.toInt(),
          ),
        )
        log.debug("Saved interesting event ID ${saved.eventReviewId} - ${mergedEvent.eventType()} - replaced ${mergedEvent.removedPrisonerNumber()} with ${mergedEvent.prisonerNumber()}")
      } else {
        log.debug("Ignoring offender merged event for ${mergedEvent.removedPrisonerNumber()} - prison ${prisoner.agencyId} is not rolled out.")
      }
    }

    return Outcome.success()
  }

  private fun InboundEvent.getEventMessage(prisoner: InmateDetail): String {
    val prisonerDetails = "${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"

    return when (this) {
      is ActivitiesChangedEvent -> "Activities changed '${action()?.name}' from prison ${this.prisonCode()}, for $prisonerDetails"
      is AlertsUpdatedEvent -> "Alerts updated for $prisonerDetails"
      is AppointmentsChangedEvent -> "Appointments changed '${additionalInformation.action}' from prison ${this.prisonCode()}, for $prisonerDetails"
      is CellMoveEvent -> "Cell move for $prisonerDetails"
      is NonAssociationsChangedEvent -> "Non-associations for $prisonerDetails"
      is IncentivesInsertedEvent -> "Incentive review created for $prisonerDetails"
      is IncentivesUpdatedEvent -> "Incentive review updated for $prisonerDetails"
      is IncentivesDeletedEvent -> "Incentive review deleted for $prisonerDetails"
      is PrisonerReceivedEvent -> "Prisoner received into prison ${prisoner.agencyId}, $prisonerDetails"
      is PrisonerReleasedEvent -> "Prisoner released from prison ${this.prisonCode()}, $prisonerDetails"
      is OffenderMergedEvent -> "Prisoner ${prisoner.firstName} ${prisoner.lastName} merged from ${this.removedPrisonerNumber()} to ${this.prisonerNumber()}"
      else -> "Unknown event for $prisonerDetails"
    }
  }

  private fun getPrisonerDetailsFor(prisonerNumber: String) = prisonApiClient.getPrisonerDetailsLite(prisonerNumber)
}

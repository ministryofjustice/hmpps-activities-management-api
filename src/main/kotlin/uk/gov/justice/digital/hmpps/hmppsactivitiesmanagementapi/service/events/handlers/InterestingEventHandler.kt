package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReviewDescription
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.Action
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ActivitiesChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AlertsUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundReleaseEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.IncentivesInsertedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.IncentivesUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.NonAssociationsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderMergedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerReceivedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerUpdatedEvent
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
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiAppWebClient: PrisonerSearchApiClient,
) : EventHandler<InboundEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private fun withPrisoner(
    prisonerNumber: String,
    onFound: (Prisoner) -> Outcome,
    onMissing: () -> Outcome = { Outcome.success() },
    onServerError: () -> Outcome = { Outcome.failed() },
  ): Outcome = try {
    val prisoner = prisonerSearchApiAppWebClient.findByPrisonerNumber(prisonerNumber)
    if (prisoner != null) onFound(prisoner) else onMissing()
  } catch (e: WebClientResponseException) {
    when {
      e.statusCode == HttpStatus.NOT_FOUND -> onMissing()
      e.statusCode.is5xxServerError -> onServerError()
      else -> Outcome.failed()
    }
  }

  // Ignore stale alert removals for merged prisoners whose old number no longer exists.
  private fun AlertsUpdatedEvent.isStaleAlertRemovalOfMergedPrisoner() = additionalInformation.alertsAdded.isEmpty() &&
    additionalInformation.alertsRemoved.isNotEmpty()

  override fun handle(event: InboundEvent): Outcome {
    log.debug("Checking for interesting event: {}", event)

    if (!event.isMeaningful()) {
      return Outcome.success()
    }

    if (event is InboundReleaseEvent) return recordRelease(event)
    if (event is OffenderMergedEvent) return recordMerge(event)

    return withPrisoner(
      prisonerNumber = event.prisonerNumber(),
      onFound = { prisoner ->
        prisoner.prisonId?.let { agencyId ->
          if (rolloutPrisonService.isActivitiesRolledOutAt(agencyId)) {
            if (allocationRepository.findByPrisonCodePrisonerNumberPrisonerStatus(
                prisonCode = agencyId,
                prisonerNumber = event.prisonerNumber(),
                prisonerStatus = arrayOf(PrisonerStatus.ACTIVE, PrisonerStatus.PENDING),
              ).isNotEmpty()
            ) {
              val eventData = buildEventData(event, prisoner) ?: return@withPrisoner Outcome.success()
              val saved = eventReviewRepository.saveAndFlush(
                EventReview(
                  eventTime = LocalDateTime.now(),
                  eventType = event.eventType(),
                  eventData = eventData,
                  prisonCode = agencyId,
                  prisonerNumber = event.prisonerNumber(),
                  bookingId = prisoner.bookingId?.toInt(),
                  eventDescription = event.getEventDesc(),
                ),
              )
              log.debug("Saved interesting event ID ${saved.eventReviewId} - ${event.eventType()} - for ${event.prisonerNumber()}")
              return@withPrisoner Outcome.success()
            } else {
              log.info("${event.prisonerNumber()} has no active or pending allocations at $agencyId")
              return@withPrisoner Outcome.success()
            }
          } else {
            log.debug("$agencyId is not a rolled out prison")
            return@withPrisoner Outcome.success()
          }
        }
        // Prisoner search was successful, but without a prisonId there is no prison context
        // to match against, so this event is treated as non-applicable.
        Outcome.success()
      },
      onMissing = {
        if (event is AlertsUpdatedEvent && event.isStaleAlertRemovalOfMergedPrisoner()) {
          log.info("Ignoring stale alerts update for prisoner {}", event.prisonerNumber())
          Outcome.success()
        } else {
          Outcome.failed()
        }
      },
      onServerError = {
        Outcome.failed()
      },
    )
  }

  private fun buildEventData(event: InboundEvent, prisoner: Prisoner): String? {
    if (event !is PrisonerUpdatedEvent || !event.isCellMove()) {
      return event.eventMessage()
    }

    return buildCellMoveEventData(prisoner.bookingId?.toLong())
  }

  private fun buildCellMoveEventData(bookingId: Long?): String? {
    if (bookingId == null) return null

    val cellHistory = runBlocking { prisonApiClient.getCellHistoryForPrisoner(bookingId) }
    val currentAssignment = cellHistory.firstOrNull { it.assignmentEndDateTime == null } ?: return null
    val previousAssignment = currentAssignment.bedAssignmentHistorySequence
      ?.minus(1)
      ?.let { previousSequence ->
        cellHistory.firstOrNull {
          it.bedAssignmentHistorySequence == previousSequence && !it.description.isNullOrBlank()
        }
      }

    val currentCell = currentAssignment.description?.trim()?.takeIf(String::isNotEmpty)
    val previousCell = previousAssignment?.description?.trim()?.takeIf(String::isNotEmpty)

    return when {
      currentCell == null && previousCell == null -> null
      else -> "${previousCell ?: "None"};${currentCell.orEmpty()}"
    }
  }

  private fun recordRelease(releaseEvent: InboundReleaseEvent): Outcome {
    if (!rolloutPrisonService.isActivitiesRolledOutAt(releaseEvent.prisonCode())) {
      log.debug("${releaseEvent.prisonCode()} is not a rolled out prison")
      return Outcome.success()
    }
    return withPrisoner(
      prisonerNumber = releaseEvent.prisonerNumber(),
      onFound = { prisoner ->
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
        log.debug(
          "Saved interesting event ID ${saved.eventReviewId} - ${releaseEvent.eventType()} - for ${releaseEvent.prisonerNumber()}",
        )
        Outcome.success()
      },
      onMissing = { Outcome.success() },
      onServerError = { Outcome.failed() },
    )
  }

  private fun recordMerge(mergedEvent: OffenderMergedEvent): Outcome = withPrisoner(
    prisonerNumber = mergedEvent.prisonerNumber(),
    onFound = { prisoner ->
      prisoner.prisonId?.let { agencyId ->
        if (rolloutPrisonService.isActivitiesRolledOutAt(agencyId)) {
          val saved = eventReviewRepository.saveAndFlush(
            EventReview(
              eventTime = LocalDateTime.now(),
              eventType = mergedEvent.eventType(),
              eventData = mergedEvent.eventMessage(),
              prisonCode = agencyId,
              prisonerNumber = mergedEvent.prisonerNumber(),
              bookingId = prisoner.bookingId?.toInt(),
              eventDescription = mergedEvent.getEventDesc(),
            ),
          )
          log.debug(
            "Saved interesting event ID ${saved.eventReviewId} - ${mergedEvent.eventType()} - replaced ${mergedEvent.removedPrisonerNumber()} with ${mergedEvent.prisonerNumber()}",
          )
        } else {
          log.debug(
            "Ignoring offender merged event for ${mergedEvent.removedPrisonerNumber()} - prison $agencyId is not rolled out.",
          )
        }
      }
      Outcome.success()
    },
    onMissing = { Outcome.success() },
    onServerError = { Outcome.failed() }, // 500s should fail so retries can happen.
  )
  private fun InboundEvent.getEventDesc(): EventReviewDescription? = when (this) {
    is ActivitiesChangedEvent ->
      when (action()) {
        Action.END -> EventReviewDescription.ACTIVITY_ENDED
        Action.SUSPEND -> EventReviewDescription.ACTIVITY_SUSPENDED
        else -> null
      }
    is AppointmentsChangedEvent ->
      when (action()) {
        Action.YES -> EventReviewDescription.APPOINTMENTS_CANCELLED
        Action.NO -> EventReviewDescription.APPOINTMENTS_KEPT
        else -> null
      }
    is PrisonerReleasedEvent ->
      if (isTransferred()) {
        EventReviewDescription.TRANSFER_OUT
      } else if (isPermanent() || isTemporary()) {
        EventReviewDescription.RELEASED
      } else {
        null
      }
    is AlertsUpdatedEvent ->
      when {
        hasAlertsAdded() && hasAlertsRemoved() -> EventReviewDescription.ALERTS_ADDED_AND_CLOSED
        hasAlertsAdded() -> EventReviewDescription.ALERT_ADDED
        hasAlertsRemoved() -> EventReviewDescription.ALERT_CLOSED
        else -> null
      }
    is PrisonerReceivedEvent -> EventReviewDescription.ARRIVAL_OR_RETURN
    is NonAssociationsChangedEvent -> EventReviewDescription.NON_ASSOCIATION
    is PrisonerUpdatedEvent -> if (isCellMove()) EventReviewDescription.CELL_MOVE else null
    is IncentivesInsertedEvent -> EventReviewDescription.INCENTIVE_LEVEL_CHANGED
    is IncentivesUpdatedEvent -> EventReviewDescription.INCENTIVE_LEVEL_CHANGED
    is OffenderMergedEvent -> EventReviewDescription.PRISONER_MERGED
    else -> null
  }
}

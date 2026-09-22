package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
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
  private fun AlertsUpdatedEvent.isStaleRemoval() = additionalInformation.alertsAdded.isEmpty() &&
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
              val saved = eventReviewRepository.saveAndFlush(
                EventReview(
                  eventTime = LocalDateTime.now(),
                  eventType = event.eventType(),
                  eventData = event.eventMessage(),
                  prisonCode = agencyId,
                  prisonerNumber = event.prisonerNumber(),
                  bookingId = prisoner.bookingId?.toInt(),
                ),
              )
              log.debug("Saved interesting event ID ${saved.eventReviewId} - ${event.eventType()} - for ${event.prisonerNumber()}")
              return@withPrisoner Outcome.success()
            } else {
              log.info("${event.prisonerNumber()} has no active or pending allocations at $agencyId")
            }
          } else {
            log.debug("$agencyId is not a rolled out prison")
          }
        }

        Outcome.failed()
      },
      onMissing = {
        if (event is AlertsUpdatedEvent && event.isStaleRemoval()) {
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
}

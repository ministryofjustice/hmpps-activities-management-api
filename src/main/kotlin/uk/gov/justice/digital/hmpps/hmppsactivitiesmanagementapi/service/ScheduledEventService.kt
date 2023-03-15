package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformPrisonerScheduledActivityToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformToPrisonerScheduledEvents
import java.time.LocalDate

@Service
class ScheduledEventService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository,
  private val prisonRegimeService: PrisonRegimeService,
  private val appointmentInstanceService: AppointmentInstanceService,
) {
  /**
   *  Get scheduled events for a prison, a single prisoner, between two dates and with an optional time slot.
   *  Court hearings, appointments and visits are from Prison API
   *  Activities are from the Activities database if rolled out is true, else from Prison API.
   */
  fun getScheduledEventsByPrisonAndPrisonerAndDateRange(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange,
    slot: TimeSlot? = null,
  ) = prisonerSearchApiClient.findByPrisonerNumbers(listOf(prisonerNumber)).block()?.firstOrNull()
    .also {
      if (it == null) {
        throw EntityNotFoundException("Prisoner '$prisonerNumber' not found")
      }
      if (it.prisonId != prisonCode || it.bookingId == null) {
        throw EntityNotFoundException("Prisoner '$prisonerNumber' not found in prison '$prisonCode'")
      }
    }
    ?.let { prisonerDetail ->
      val bookingId = prisonerDetail.bookingId!!.toLong()
      val prisonRolledOut = rolloutPrisonRepository.findByCode(prisonCode)
        ?: throw EntityNotFoundException("Unable to get scheduled events. Could not find prison with code $prisonCode")
      val eventPriorities = prisonRegimeService.getEventPrioritiesForPrison(prisonCode)
      getSinglePrisonerEventCalls(bookingId, prisonRolledOut, dateRange)
        .map { t ->
          transformToPrisonerScheduledEvents(
            bookingId,
            prisonCode,
            prisonerNumber,
            dateRange,
            eventPriorities,
            t.t1, // Appointments
            t.t2, // Court hearings
            t.t3, // Visits
            t.t4, // Activities,
          )
        }.block()
        ?.apply {
          if (prisonRolledOut.active) {
            activities = transformPrisonerScheduledActivityToScheduledEvents(
              prisonCode,
              EventType.ACTIVITY.defaultPriority,
              eventPriorities[EventType.ACTIVITY],
              getSinglePrisonerScheduledActivities(prisonCode, prisonerNumber, dateRange, slot),
            )
          }
        }
    }

  private fun getSinglePrisonerEventCalls(bookingId: Long, prisonRolledOut: RolloutPrison, dateRange: LocalDateRange) =

    Mono.zip(
      Mono.just(appointmentInstanceService.getScheduledEvents(prisonRolledOut, bookingId, dateRange)),
      prisonApiClient.getScheduledCourtHearings(bookingId, dateRange),
      prisonApiClient.getScheduledVisits(bookingId, dateRange),
      if (!prisonRolledOut.active) {
        prisonApiClient.getScheduledActivities(bookingId, dateRange)
      } else {
        Mono.just(emptyList())
      },
    )

  private fun getSinglePrisonerScheduledActivities(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange,
    slot: TimeSlot?,
  ): List<PrisonerScheduledActivity> {
    val activities = prisonerScheduledActivityRepository
      .getScheduledActivitiesForPrisonerAndDateRange(
        prisonCode,
        prisonerNumber,
        dateRange.start,
        dateRange.endInclusive,
      )

    return if (slot != null) {
      activities.filter {
        TimeSlot.slot(it.startTime!!) == slot
      }
    } else {
      activities
    }
  }

  /**
   * Get the scheduled events for a list of prisoner numbers, for one date and time slot
   * Court hearings, appointments and visits are from Prison API
   * Activities are from the Activities database if rolled out is true, else from Prison API.
   */
  fun getScheduledEventsByPrisonAndPrisonersAndDateRange(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot? = null,
  ): PrisonerScheduledEvents? {
    val eventPriorities = prisonRegimeService.getEventPrioritiesForPrison(prisonCode)
    val prisonRolledOut = rolloutPrisonRepository.findByCode(prisonCode)
      ?: throw EntityNotFoundException("Unable to get scheduled events. Could not find prison with code $prisonCode")
    return getMultiplePrisonerEventCalls(prisonCode, prisonerNumbers, prisonRolledOut, date, timeSlot)
      .map { t ->
        transformToPrisonerScheduledEvents(
          prisonCode,
          prisonerNumbers,
          date,
          eventPriorities,
          t.t1, // Appointments
          t.t2, // Court hearings
          t.t3, // Visits
          t.t4, // Activities,
          t.t5, // External transfers
        )
      }.block()
      ?.apply {
        if (prisonRolledOut.active) {
          activities = transformPrisonerScheduledActivityToScheduledEvents(
            prisonCode,
            EventType.ACTIVITY.defaultPriority,
            eventPriorities[EventType.ACTIVITY],
            getMultiplePrisonerScheduledActivities(prisonCode, prisonerNumbers, date, timeSlot),
          )
        }
      }
  }

  private fun getMultiplePrisonerEventCalls(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    rolloutPrison: RolloutPrison,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ) =
    Mono.zip(
      Mono.just(
        appointmentInstanceService.getPrisonerSchedules(
          prisonCode,
          prisonerNumbers,
          rolloutPrison,
          date,
          timeSlot,
        ),
      ),
      prisonApiClient.getScheduledCourtEventsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot),
      prisonApiClient.getScheduledVisitsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot),
      fetchPrisonApiActivitiesIfPrisonNotRolledOut(rolloutPrison, prisonCode, prisonerNumbers, date, timeSlot),
      fetchPrisonApiExternalTransfersIfDateIsToday(date, prisonCode, prisonerNumbers),
    )

  private fun fetchPrisonApiActivitiesIfPrisonNotRolledOut(
    rolloutPrison: RolloutPrison,
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ) =
    ifTrue(!rolloutPrison.active) {
      prisonApiClient.getScheduledActivitiesForPrisonerNumbers(
        prisonCode,
        prisonerNumbers,
        date,
        timeSlot,
      )
    }

  private fun fetchPrisonApiExternalTransfersIfDateIsToday(
    date: LocalDate,
    prisonCode: String,
    prisonerNumbers: Set<String>,
  ) = ifTrue(date == LocalDate.now()) {
    prisonApiClient.getExternalTransfersOnDate(prisonCode, prisonerNumbers, date)
      .map { transfers -> transfers.map { transfer -> transfer.redacted() } }
  }

  private fun <T> ifTrue(condition: Boolean, f: () -> Mono<List<T>>) = if (condition) f() else Mono.just(emptyList())

  private fun PrisonerSchedule.redacted() = this.copy(
    comment = null,
    locationCode = null,
    locationId = null,
    eventLocationId = null,
    eventLocation = null,
    eventDescription = "",
  )

  private fun getMultiplePrisonerScheduledActivities(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    slot: TimeSlot?,
  ): List<PrisonerScheduledActivity> {
    val activities = prisonerScheduledActivityRepository
      .getScheduledActivitiesForPrisonerListAndDate(prisonCode, prisonerNumbers, date)

    return if (slot != null) {
      activities.filter {
        TimeSlot.slot(it.startTime!!) == slot
      }
    } else {
      activities
    }
  }
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformPrisonerScheduledActivityToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformToPrisonerScheduledEvents
import java.time.LocalDate
import javax.persistence.EntityNotFoundException

@Service
class ScheduledEventService(
  private val prisonApiClient: PrisonApiClient,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository,
  private val prisonRegimeService: PrisonRegimeService
) {
  /**
   *  Get scheduled events for a prison, a single prisoner., between two dates, with an optional time slot.
   *  Court hearings, appointments and visits are from Prison API
   *  Activities are from the Activities database if rolled out is true, else from Prison API.
   */
  public fun getScheduledEventsByPrisonAndPrisonerAndDateRange(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange,
    slot: TimeSlot? = null,
    // TODO: Change this to use the prisonerSearchAPI to get the booking ID! This endpoint will be deprecated soon.
  ) = prisonApiClient.getPrisonerDetails(prisonerNumber).block()
    ?.also {
      if (it.agencyId != prisonCode || it.bookingId == null) {
        throw EntityNotFoundException("Prisoner '$prisonerNumber' not found in prison '$prisonCode'")
      }
    }
    ?.let { prisonerDetail ->
      val prisonRolledOut = rolloutPrisonRepository.findByCode(prisonCode).let {
        it != null && it.active
      }
      val eventPriorities = prisonRegimeService.getEventPrioritiesForPrison(prisonCode)
      getSinglePrisonerEventCalls(prisonerDetail.bookingId!!, prisonRolledOut, dateRange)
        .map { t ->
          transformToPrisonerScheduledEvents(
            prisonerDetail.bookingId,
            prisonCode,
            prisonerNumber,
            dateRange,
            eventPriorities,
            t.t1, // Appointments
            t.t2, // Court hearings
            t.t3, // Visits
            t.t4, // Activities
          )
        }.block()
        ?.apply {
          if (prisonRolledOut) {
            activities = transformPrisonerScheduledActivityToScheduledEvents(
              prisonCode,
              EventType.ACTIVITY.defaultPriority,
              eventPriorities[EventType.ACTIVITY],
              getSinglePrisonerScheduledActivities(prisonCode, prisonerNumber, dateRange, slot)
            )
          }
        }
    }

  private fun getSinglePrisonerEventCalls(bookingId: Long, prisonRolledOut: Boolean, dateRange: LocalDateRange) =
    Mono.zip(
      prisonApiClient.getScheduledAppointments(bookingId, dateRange),
      prisonApiClient.getScheduledCourtHearings(bookingId, dateRange),
      prisonApiClient.getScheduledVisits(bookingId, dateRange),
      if (!prisonRolledOut) {
        prisonApiClient.getScheduledActivities(bookingId, dateRange)
      } else {
        Mono.just(emptyList())
      }
    )

  private fun getSinglePrisonerScheduledActivities(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange,
    slot: TimeSlot?,
  ): List<PrisonerScheduledActivity> {
    val activities = prisonerScheduledActivityRepository
      .getScheduledActivitiesForPrisonerAndDateRange(prisonCode, prisonerNumber, dateRange.start, dateRange.endInclusive)

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
  public fun getScheduledEventsByPrisonAndPrisonersAndDateRange(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot? = null,
  ): PrisonerScheduledEvents? {
    val eventPriorities = prisonRegimeService.getEventPrioritiesForPrison(prisonCode)
    val prisonRolledOut = rolloutPrisonRepository.findByCode(prisonCode).let {
      it != null && it.active
    }
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
          t.t4, // Activities
        )
      }.block()
      ?.apply {
        if (prisonRolledOut) {
          activities = transformPrisonerScheduledActivityToScheduledEvents(
            prisonCode,
            EventType.ACTIVITY.defaultPriority,
            eventPriorities[EventType.ACTIVITY],
            getMultiplePrisonerScheduledActivities(prisonCode, prisonerNumbers, date, timeSlot)
          )
        }
      }
  }

  private fun getMultiplePrisonerEventCalls(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    prisonRolledOut: Boolean,
    date: LocalDate,
    timeSlot: TimeSlot?
  ) =
    Mono.zip(
      prisonApiClient.getScheduledAppointmentsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot),
      prisonApiClient.getScheduledCourtEventsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot),
      prisonApiClient.getScheduledVisitsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot),
      if (!prisonRolledOut) {
        prisonApiClient.getScheduledActivitiesForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot)
      } else {
        Mono.just(emptyList())
      }
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

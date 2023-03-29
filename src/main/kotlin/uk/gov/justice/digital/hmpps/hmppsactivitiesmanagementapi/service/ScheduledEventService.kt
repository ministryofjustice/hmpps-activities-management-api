package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.rangeTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.prisonApiActivitiesToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.prisonApiAppointmentsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.prisonApiCourtEventsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.prisonApiCourtHearingsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.prisonApiOffenderAdjudicationsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.prisonApiScheduledEventToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.prisonApiTransfersToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.prisonApiVisitsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformPrisonerScheduledActivityToScheduledEvents
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

typealias BookingIdPrisonerNo = Pair<Long, String>

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
   *  Court hearings, visits and external transfers are from Prison API
   *  Appointments are from the SAA DB if "prisonRollout.appointmentsDataSource" == 'ACTIVITIES' else from Prison API.
   *  Activities are from the SAA DB if "prisonRollout.active" is true else from Prison API.
   */
  fun getScheduledEventsByPrisonAndPrisonerAndDateRange(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange,
    slot: TimeSlot? = null,
  ) = runBlocking {
    prisonerSearchApiClient.findByPrisonerNumbersAsync(listOf(prisonerNumber)).firstOrNull()
      .also { prisoner ->
        if (prisoner == null) {
          throw EntityNotFoundException("Prisoner '$prisonerNumber' not found")
        }
        if (prisoner.prisonId != prisonCode || prisoner.bookingId == null) {
          throw EntityNotFoundException("Prisoner '$prisonerNumber' not found in prison '$prisonCode'")
        }
      }
      ?.let { prisonerDetail ->
        val bookingId = prisonerDetail.bookingId!!.toLong()
        val prisonRolledOut = rolloutPrisonRepository.findByCode(prisonCode)
          ?: throw EntityNotFoundException("Unable to get scheduled events. Could not find prison with code $prisonCode")
        val eventPriorities = prisonRegimeService.getEventPrioritiesForPrison(prisonCode)
        getSinglePrisonerEventCalls(BookingIdPrisonerNo(bookingId, prisonerNumber), prisonRolledOut, dateRange)
          .let { schedules ->
            PrisonerScheduledEvents(
              prisonCode,
              setOf(prisonerNumber),
              dateRange.start,
              dateRange.endInclusive,
              schedules.appointments.prisonApiScheduledEventToScheduledEvents(
                prisonerNumber,
                EventType.APPOINTMENT.name,
                EventType.APPOINTMENT.defaultPriority,
                eventPriorities[EventType.APPOINTMENT],
              ),
              schedules.courtHearings.prisonApiCourtHearingsToScheduledEvents(
                bookingId,
                prisonCode,
                prisonerNumber,
                EventType.COURT_HEARING.name,
                EventType.COURT_HEARING.defaultPriority,
                eventPriorities[EventType.COURT_HEARING],
              ),
              schedules.visits.prisonApiScheduledEventToScheduledEvents(
                prisonerNumber,
                EventType.VISIT.name,
                EventType.VISIT.defaultPriority,
                eventPriorities[EventType.VISIT],
              ),
              schedules.activities.prisonApiScheduledEventToScheduledEvents(
                prisonerNumber,
                EventType.ACTIVITY.name,
                EventType.ACTIVITY.defaultPriority,
                eventPriorities[EventType.ACTIVITY],
              ),
              schedules.transfers.prisonApiTransfersToScheduledEvents(
                prisonCode,
                eventPriorities[EventType.EXTERNAL_TRANSFER],
              ),
              schedules.adjudications.prisonApiOffenderAdjudicationsToScheduledEvents(
                prisonCode,
                eventPriorities[EventType.ADJUDICATION_HEARING],
              ),
            )
          }
          .apply {
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
  }

  private data class SinglePrisonerSchedules(
    val appointments: List<PrisonApiScheduledEvent>,
    val courtHearings: CourtHearings,
    val visits: List<PrisonApiScheduledEvent>,
    val activities: List<PrisonApiScheduledEvent>,
    val transfers: List<PrisonerSchedule>,
    val adjudications: List<OffenderAdjudicationHearing>,
  )

  /**
   * Makes async calls to prison API to gather the events which appear on prisoner schedules.
   */
  private suspend fun getSinglePrisonerEventCalls(
    prisoner: BookingIdPrisonerNo,
    prisonRolledOut: RolloutPrison,
    dateRange: LocalDateRange,
  ): SinglePrisonerSchedules = coroutineScope {
    val appointments = async {
      appointmentInstanceService.getScheduledEvents(prisonRolledOut, prisoner.first, dateRange)
    }

    val courtHearings = async {
      prisonApiClient.getScheduledCourtHearingsAsync(prisoner.first, dateRange)
    }

    val visits = async {
      prisonApiClient.getScheduledVisitsAsync(prisoner.first, dateRange)
    }

    val activities = async {
      if (prisonRolledOut.active) {
        emptyList()
      } else {
        prisonApiClient.getScheduledActivitiesAsync(prisoner.first, dateRange)
      }
    }

    val transfers = async {
      fetchPrisonApiExternalTransfersIfRangeIncludesToday(prisonRolledOut, prisoner.second, dateRange)
    }

    val adjudications =
      async { prisonApiClient.getOffenderAdjudications(prisonRolledOut.code, dateRange, setOf(prisoner.second)) }

    SinglePrisonerSchedules(
      appointments.await(),
      courtHearings.await(),
      visits.await(),
      activities.await(),
      transfers.await(),
      adjudications.await(),
    )
  }

  private suspend fun fetchPrisonApiExternalTransfersIfRangeIncludesToday(
    rolloutPrison: RolloutPrison,
    prisonerNumber: String,
    dateRange: LocalDateRange,
  ): List<PrisonerSchedule> = LocalDate.now()
    .takeIf { dateRange.includes(it) }
    ?.let { today ->
      fetchPrisonApiExternalTransfersIfDateIsToday(today, rolloutPrison.code, setOf(prisonerNumber))
    }
    ?: emptyList()

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

    return if (slot != null) activities.filter { TimeSlot.slot(it.startTime!!) == slot } else activities
  }

  /**
   * Get the scheduled events for a list of prisoner numbers, for one date and time slot
   * Court hearings, visits and external transfers are from Prison API
   * Appointments are from the SAA DB if prisonRollout appointmentsDataSource == 'ACTIVITIES', else from Prison API.
   * Activities are from the SAA DB if prisonRollout active is true, else from Prison API.
   */
  fun getScheduledEventsByPrisonAndPrisonersAndDateRange(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot? = null,
  ): PrisonerScheduledEvents? = runBlocking {
    // Await completion - not async - will always return priorities (defaults set)
    val eventPriorities = withContext(Dispatchers.IO) {
      prisonRegimeService.getEventPrioritiesForPrison(prisonCode)
    }

    // Await completion - not async
    val prisonRolledOut = withContext(Dispatchers.IO) {
      rolloutPrisonRepository.findByCode(prisonCode)
    }
      ?: throw EntityNotFoundException("Unable to get scheduled events. Could not find prison with code $prisonCode")

    getMultiplePrisonerEventCalls(prisonRolledOut, prisonerNumbers, date, timeSlot)
      .let { schedules ->
        PrisonerScheduledEvents(
          prisonCode,
          prisonerNumbers,
          date,
          date,
          schedules.appointments.prisonApiAppointmentsToScheduledEvents(
            prisonCode,
            eventPriorities[EventType.APPOINTMENT],
          ),
          schedules.courtEvents.prisonApiCourtEventsToScheduledEvents(
            prisonCode,
            eventPriorities[EventType.COURT_HEARING],
          ),
          schedules.visits.prisonApiVisitsToScheduledEvents(
            prisonCode,
            eventPriorities[EventType.VISIT],
          ),
          schedules.activities.prisonApiActivitiesToScheduledEvents(
            prisonCode,
            eventPriorities[EventType.ACTIVITY],
          ),
          schedules.transfers.prisonApiTransfersToScheduledEvents(
            prisonCode,
            eventPriorities[EventType.EXTERNAL_TRANSFER],
          ),
          schedules.adjudications.prisonApiOffenderAdjudicationsToScheduledEvents(
            prisonCode,
            eventPriorities[EventType.ADJUDICATION_HEARING],
          ),
        )
      }
      .apply {
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

  private data class MultiPrisonerSchedules(
    val appointments: List<PrisonerSchedule>,
    val courtEvents: List<PrisonerSchedule>,
    val visits: List<PrisonerSchedule>,
    val activities: List<PrisonerSchedule>,
    val transfers: List<PrisonerSchedule>,
    val adjudications: List<OffenderAdjudicationHearing>,
  )

  private suspend fun getMultiplePrisonerEventCalls(
    rolloutPrison: RolloutPrison,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): MultiPrisonerSchedules = coroutineScope {
    val appointments = async {
      appointmentInstanceService.getPrisonerSchedules(
        rolloutPrison.code,
        prisonerNumbers,
        rolloutPrison,
        date,
        timeSlot,
      )
    }

    val courtEvents = async {
      prisonApiClient.getScheduledCourtEventsForPrisonerNumbersAsync(
        rolloutPrison.code,
        prisonerNumbers,
        date,
        timeSlot,
      )
    }

    val visits = async {
      prisonApiClient.getScheduledVisitsForPrisonerNumbersAsync(
        rolloutPrison.code,
        prisonerNumbers,
        date,
        timeSlot,
      )
    }

    val activities = async {
      fetchPrisonApiActivitiesIfPrisonNotRolledOut(
        rolloutPrison,
        prisonerNumbers,
        date,
        timeSlot,
      )
    }

    val transfers = async {
      fetchPrisonApiExternalTransfersIfDateIsToday(
        date,
        rolloutPrison.code,
        prisonerNumbers,
      )
    }

    val adjudications = async {
      prisonApiClient.getOffenderAdjudications(
        rolloutPrison.code,
        date.rangeTo(date.plusDays(1)),
        prisonerNumbers,
        timeSlot,
      )
    }

    MultiPrisonerSchedules(
      appointments.await(),
      courtEvents.await(),
      visits.await(),
      activities.await(),
      transfers.await(),
      adjudications.await(),
    )
  }

  private suspend fun fetchPrisonApiActivitiesIfPrisonNotRolledOut(
    rolloutPrison: RolloutPrison,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ) =
    if (!rolloutPrison.active) {
      prisonApiClient.getScheduledActivitiesForPrisonerNumbersAsync(
        rolloutPrison.code,
        prisonerNumbers,
        date,
        timeSlot,
      )
    } else {
      emptyList()
    }

  private suspend fun fetchPrisonApiExternalTransfersIfDateIsToday(
    date: LocalDate,
    prisonCode: String,
    prisonerNumbers: Set<String>,
  ) = if (date == LocalDate.now()) {
    prisonApiClient.getExternalTransfersOnDateAsync(prisonCode, prisonerNumbers, date)
      .map { transfers -> transfers.redacted() }
  } else {
    emptyList()
  }

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
    val activities = prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
      prisonCode,
      prisonerNumbers,
      date,
    )
    return if (slot != null) activities.filter { TimeSlot.slot(it.startTime!!) == slot } else activities
  }
}

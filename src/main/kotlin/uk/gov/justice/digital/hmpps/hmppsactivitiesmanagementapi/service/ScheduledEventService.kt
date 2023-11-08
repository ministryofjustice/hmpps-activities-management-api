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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.rangeTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.multiplePrisonerActivitiesToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.multiplePrisonerAppointmentsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.multiplePrisonerCourtEventsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.multiplePrisonerTransfersToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.multiplePrisonerVisitsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.nomisActivitiesToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.nomisAdjudicationsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.nomisAppointmentsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.nomisCourtHearingsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.nomisVisitsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformAppointmentInstanceToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformPrisonerScheduledActivityToScheduledEvents
import java.time.LocalDate
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

typealias BookingIdPrisonerNo = Pair<Long, String>

@Service
class ScheduledEventService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val prisonRegimeService: PrisonRegimeService,
) {
  /**
   *  Get scheduled events for a single prisoner, between two dates and with an optional time slot.
   *  Court hearings, visits, adjudications and external transfers are from NOMIS via Prison API.
   *  Appointments are either from the local DB if appointments are active, else from NOMIS via Prison API.
   *  Activities are either from the local DB if activities are active, else from NOMIS via Prison API.
   */
  fun getScheduledEventsForSinglePrisoner(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange,
    slot: TimeSlot? = null,
    referenceCodesForAppointmentsMap: Map<String, ReferenceCode> = emptyMap(),
    locationsForAppointmentsMap: Map<Long, Location> = emptyMap(),
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
        val prisonLocations = prisonApiClient.getEventLocationsForPrison(prisonCode)

        getSinglePrisonerEventCalls(BookingIdPrisonerNo(bookingId, prisonerNumber), prisonRolledOut, dateRange)
          .let { schedules ->
            PrisonerScheduledEvents(
              prisonCode,
              setOf(prisonerNumber),
              dateRange.start,
              dateRange.endInclusive,
              schedules.appointments.nomisAppointmentsToScheduledEvents(
                prisonerNumber,
                eventPriorities.getOrDefault(EventType.APPOINTMENT),
                prisonLocations,
              ),
              schedules.courtHearings.nomisCourtHearingsToScheduledEvents(
                bookingId,
                prisonCode,
                prisonerNumber,
                eventPriorities.getOrDefault(EventType.COURT_HEARING),
              ),
              schedules.visits.nomisVisitsToScheduledEvents(
                prisonerNumber,
                eventPriorities.getOrDefault(EventType.VISIT),
                prisonLocations,
              ),
              schedules.activities.nomisActivitiesToScheduledEvents(
                prisonerNumber,
                eventPriorities.getOrDefault(EventType.ACTIVITY),
                prisonLocations,
              ),
              schedules.transfers.multiplePrisonerTransfersToScheduledEvents(
                prisonCode,
                eventPriorities.getOrDefault(EventType.EXTERNAL_TRANSFER),
              ),
              schedules.adjudications.nomisAdjudicationsToScheduledEvents(
                prisonCode,
                eventPriorities.getOrDefault(EventType.ADJUDICATION_HEARING),
                prisonLocations,
              ),
            )
          }
          .apply {
            // If activities is enabled replace the empty list with the details from the local database
            if (prisonRolledOut.isActivitiesRolledOut()) {
              activities = transformPrisonerScheduledActivityToScheduledEvents(
                prisonCode,
                eventPriorities,
                getSinglePrisonerScheduledActivities(prisonCode, prisonerNumber, dateRange, slot),
                prisonLocations,
              )
            }

            // If appointments is enabled replace the empty list with the details from the local database
            if (prisonRolledOut.isAppointmentsRolledOut()) {
              appointments = transformAppointmentInstanceToScheduledEvents(
                prisonCode,
                eventPriorities,
                referenceCodesForAppointmentsMap,
                locationsForAppointmentsMap,
                getSinglePrisonerAppointments(bookingId, dateRange, slot),
              )
            }
          }
      }
  }

  private data class SinglePrisonerSchedules(
    val appointments: List<PrisonApiScheduledEvent>,
    val courtHearings: CourtHearings?,
    val visits: List<PrisonApiScheduledEvent>,
    val activities: List<PrisonApiScheduledEvent>,
    val transfers: List<PrisonerSchedule>,
    val adjudications: List<OffenderAdjudicationHearing>,
  )

  private suspend fun getSinglePrisonerEventCalls(
    prisoner: BookingIdPrisonerNo,
    prisonRolledOut: RolloutPrison,
    dateRange: LocalDateRange,
  ): SinglePrisonerSchedules = coroutineScope {
    val sensitiveEventDateRange = when {
      dateRange.start.isAfter(LocalDate.now()) -> LocalDateRange.EMPTY
      dateRange.endInclusive.isAfter(LocalDate.now()) -> LocalDateRange(dateRange.start, LocalDate.now())
      else -> dateRange
    }

    val appointments = async {
      if (prisonRolledOut.isAppointmentsRolledOut()) {
        emptyList()
      } else {
        prisonApiClient.getScheduledAppointmentsAsync(prisoner.first, dateRange)
      }
    }

    val activities = async {
      if (prisonRolledOut.isActivitiesRolledOut()) {
        emptyList()
      } else {
        prisonApiClient.getScheduledActivitiesAsync(prisoner.first, dateRange)
      }
    }

    val courtHearings = async {
      prisonApiClient.getScheduledCourtHearingsAsync(prisoner.first, sensitiveEventDateRange)
    }

    val visits = async {
      prisonApiClient.getScheduledVisitsAsync(prisoner.first, dateRange)
    }

    val transfers = async {
      fetchExternalTransfersIfRangeIncludesToday(prisonRolledOut, prisoner.second, dateRange)
    }

    val adjudications = async {
      prisonApiClient.getOffenderAdjudications(prisonRolledOut.code, dateRange, setOf(prisoner.second))
    }

    SinglePrisonerSchedules(
      appointments.await(),
      courtHearings.await(),
      visits.await(),
      activities.await(),
      transfers.await(),
      adjudications.await(),
    )
  }

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

  private fun getSinglePrisonerAppointments(
    bookingId: Long,
    dateRange: LocalDateRange,
    slot: TimeSlot?,
  ): List<AppointmentInstance> {
    val appointments = appointmentInstanceRepository
      .findByBookingIdAndDateRange(bookingId, dateRange.start, dateRange.endInclusive)

    return if (slot != null) appointments.filter { TimeSlot.slot(it.startTime) == slot } else appointments
  }

  /**
   * Get the scheduled events for a list of prisoner numbers for one date and optional time slot
   * Court hearings, visits, adjudications and external transfers are from Prison API
   * Appointments are from the local DB if appointments is enabled otherwise from Prison API.
   * Activities are from the local DB if activities is enabled otherwise from Prison API.
   */
  fun getScheduledEventsForMultiplePrisoners(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot? = null,
    referenceCodesForAppointmentsMap: Map<String, ReferenceCode>,
    locationsForAppointmentsMap: Map<Long, Location>,
  ): PrisonerScheduledEvents? = runBlocking {
    val eventPriorities = withContext(Dispatchers.IO) { prisonRegimeService.getEventPrioritiesForPrison(prisonCode) }
    val prisonLocations = prisonApiClient.getEventLocationsForPrison(prisonCode)

    val prisonRolledOut = withContext(Dispatchers.IO) {
      rolloutPrisonRepository.findByCode(prisonCode)
    } ?: throw EntityNotFoundException("Unable to get scheduled events. Could not find prison with code $prisonCode")

    getMultiplePrisonerEventCalls(prisonRolledOut, prisonerNumbers, date, timeSlot)
      .let { schedules ->
        PrisonerScheduledEvents(
          prisonCode,
          prisonerNumbers,
          date,
          date,
          schedules.appointments.multiplePrisonerAppointmentsToScheduledEvents(
            prisonCode,
            eventPriorities.getOrDefault(EventType.APPOINTMENT),
          ),
          schedules.courtEvents.multiplePrisonerCourtEventsToScheduledEvents(
            prisonCode,
            eventPriorities.getOrDefault(EventType.COURT_HEARING),
          ),
          schedules.visits.multiplePrisonerVisitsToScheduledEvents(
            prisonCode,
            eventPriorities.getOrDefault(EventType.VISIT),
            prisonLocations,
          ),
          schedules.activities.multiplePrisonerActivitiesToScheduledEvents(
            prisonCode,
            eventPriorities.getOrDefault(EventType.ACTIVITY),
          ),
          schedules.transfers.multiplePrisonerTransfersToScheduledEvents(
            prisonCode,
            eventPriorities.getOrDefault(EventType.EXTERNAL_TRANSFER),
          ),
          schedules.adjudications.nomisAdjudicationsToScheduledEvents(
            prisonCode,
            eventPriorities.getOrDefault(EventType.ADJUDICATION_HEARING),
            prisonLocations,
          ),
        )
      }
      .apply {
        // If activities is enabled replace the empty list with the details from the local database
        if (prisonRolledOut.isActivitiesRolledOut()) {
          activities = transformPrisonerScheduledActivityToScheduledEvents(
            prisonCode,
            eventPriorities,
            getMultiplePrisonerScheduledActivities(prisonCode, prisonerNumbers, date, timeSlot),
            prisonLocations,
          )
        }

        // If appointments is enabled replace the empty list with the details from the local database
        if (prisonRolledOut.isAppointmentsRolledOut()) {
          appointments = transformAppointmentInstanceToScheduledEvents(
            prisonCode,
            eventPriorities,
            referenceCodesForAppointmentsMap,
            locationsForAppointmentsMap,
            getMultiplePrisonersAppointments(prisonCode, prisonerNumbers, date, timeSlot),
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
      if (rolloutPrison.isAppointmentsRolledOut()) {
        emptyList()
      } else {
        prisonApiClient.getScheduledAppointmentsForPrisonerNumbersAsync(
          rolloutPrison.code,
          prisonerNumbers,
          date,
          timeSlot,
        )
      }
    }

    val activities = async {
      if (rolloutPrison.isActivitiesRolledOut()) {
        emptyList()
      } else {
        prisonApiClient.getScheduledActivitiesForPrisonerNumbersAsync(
          rolloutPrison.code,
          prisonerNumbers,
          date,
          timeSlot,
        )
      }
    }

    val courtEvents = async {
      if (!date.isAfter(LocalDate.now())) {
        prisonApiClient.getScheduledCourtEventsForPrisonerNumbersAsync(
          rolloutPrison.code,
          prisonerNumbers,
          date,
          timeSlot,
        )
      } else {
        emptyList()
      }
    }

    val visits = async {
      prisonApiClient.getScheduledVisitsForPrisonerNumbersAsync(
        rolloutPrison.code,
        prisonerNumbers,
        date,
        timeSlot,
      )
    }

    val transfers = async {
      fetchExternalTransfersIfDateIsToday(date, rolloutPrison.code, prisonerNumbers)
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

  private suspend fun fetchExternalTransfersIfDateIsToday(
    date: LocalDate,
    prisonCode: String,
    prisonerNumbers: Set<String>,
  ) = if (date == LocalDate.now() && prisonerNumbers.isNotEmpty()) {
    prisonApiClient.getExternalTransfersOnDateAsync(prisonCode, prisonerNumbers, date)
      .map { transfers -> transfers.redacted() }
  } else {
    emptyList()
  }

  private suspend fun fetchExternalTransfersIfRangeIncludesToday(
    rolloutPrison: RolloutPrison,
    prisonerNumber: String,
    dateRange: LocalDateRange,
  ): List<PrisonerSchedule> = LocalDate.now()
    .takeIf { dateRange.includes(it) }
    ?.let { today ->
      fetchExternalTransfersIfDateIsToday(today, rolloutPrison.code, setOf(prisonerNumber))
    }
    ?: emptyList()

  private fun PrisonerSchedule.redacted() = this.copy(
    comment = null,
    locationCode = null,
    locationId = null,
    eventLocationId = null,
    eventLocation = null,
    eventDescription = "Transfer",
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

  private fun getMultiplePrisonersAppointments(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): List<AppointmentInstance> {
    val timeRange = timeSlot?.let { prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, it) }
    val earliestStartTime = timeRange?.start ?: LocalTime.of(0, 0)
    val latestStartTime = timeRange?.end?.minusMinutes(1) ?: LocalTime.of(23, 59)
    return appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberAndDateAndTime(
      prisonCode,
      prisonerNumbers,
      date,
      earliestStartTime,
      latestStartTime,
    )
  }
}

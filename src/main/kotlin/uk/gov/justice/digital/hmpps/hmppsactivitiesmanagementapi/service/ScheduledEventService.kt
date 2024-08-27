package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.AdjudicationsHearingAdapter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService.Companion.getSlotForDayAndTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

typealias BookingIdPrisonerNo = Pair<Long, String>

@Service
class ScheduledEventService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val rolloutPrisonService: RolloutPrisonService,
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val prisonRegimeService: PrisonRegimeService,
  private val adjudicationsHearingAdapter: AdjudicationsHearingAdapter,
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
    val prisonRegime = prisonRegimeService.getPrisonRegimesByDaysOfWeek(agencyId = prisonCode)
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
        val prisonRolledOut = rolloutPrisonService.getByPrisonCode(prisonCode)
        val eventPriorities = prisonRegimeService.getEventPrioritiesForPrison(prisonCode)
        val prisonLocations = prisonApiClient.getEventLocationsForPrison(prisonCode)

        getSinglePrisonerEventCalls(
          prisoner = BookingIdPrisonerNo(bookingId, prisonerNumber),
          prisonRolledOut = prisonRolledOut,
          dateRange = dateRange,
          prisonRegime = prisonRegime,
        )
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
            if (prisonRolledOut.activitiesRolledOut) {
              activities = transformPrisonerScheduledActivityToScheduledEvents(
                prisonCode,
                eventPriorities,
                getSinglePrisonerScheduledActivities(prisonCode, prisonerNumber, dateRange, slot),
                prisonLocations,
              )
            }

            // If appointments is enabled replace the empty list with the details from the local database
            if (prisonRolledOut.appointmentsRolledOut) {
              appointments = transformAppointmentInstanceToScheduledEvents(
                prisonCode,
                eventPriorities,
                referenceCodesForAppointmentsMap,
                locationsForAppointmentsMap,
                getSinglePrisonerAppointments(
                  bookingId = bookingId, dateRange = dateRange, slot = slot, prisonRegime = prisonRegime,
                ),
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
    prisonRolledOut: RolloutPrisonPlan,
    dateRange: LocalDateRange,
    prisonRegime: Map<Set<DayOfWeek>, PrisonRegime>,
  ): SinglePrisonerSchedules = coroutineScope {
    val sensitiveEventDateRange = when {
      dateRange.start.isAfter(LocalDate.now()) -> LocalDateRange.EMPTY
      dateRange.endInclusive.isAfter(LocalDate.now()) -> LocalDateRange(dateRange.start, LocalDate.now())
      else -> dateRange
    }

    val appointments = async {
      if (prisonRolledOut.appointmentsRolledOut) {
        emptyList()
      } else {
        prisonApiClient.getScheduledAppointmentsAsync(prisoner.first, dateRange)
      }
    }

    val activities = async {
      if (prisonRolledOut.activitiesRolledOut) {
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
      adjudicationsHearingAdapter.getAdjudicationHearings(
        agencyId = prisonRolledOut.prisonCode,
        date = dateRange.start,
        prisonerNumbers = setOf(prisoner.second),
        prisonRegime = prisonRegime,
      )
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
  ): List<PrisonerScheduledActivity> = prisonerScheduledActivityRepository
    .getScheduledActivitiesForPrisonerAndDateRange(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      startDate = dateRange.start,
      endDate = dateRange.endInclusive,
      timeSlot = slot,
    )

  private fun getSinglePrisonerAppointments(
    bookingId: Long,
    dateRange: LocalDateRange,
    slot: TimeSlot?,
    prisonRegime: Map<Set<DayOfWeek>, PrisonRegime>,
  ): List<AppointmentInstance> {
    val appointments = appointmentInstanceRepository
      .findByBookingIdAndDateRange(bookingId, dateRange.start, dateRange.endInclusive)

    return if (slot != null) appointments.filter { prisonRegime.getSlotForDayAndTime(time = it.startTime, day = it.appointmentDate.dayOfWeek) == slot } else appointments
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
      rolloutPrisonService.getByPrisonCode(prisonCode)
    }

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
        if (prisonRolledOut.activitiesRolledOut) {
          activities = transformPrisonerScheduledActivityToScheduledEvents(
            prisonCode,
            eventPriorities,
            getMultiplePrisonerScheduledActivities(prisonCode, prisonerNumbers, date, timeSlot),
            prisonLocations,
          )
        }

        // If appointments is enabled replace the empty list with the details from the local database
        if (prisonRolledOut.appointmentsRolledOut) {
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
    rolloutPrison: RolloutPrisonPlan,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): MultiPrisonerSchedules = coroutineScope {
    val prisonRegime = prisonRegimeService.getPrisonRegimesByDaysOfWeek(agencyId = rolloutPrison.prisonCode)

    val appointments = async {
      if (rolloutPrison.appointmentsRolledOut) {
        emptyList()
      } else {
        prisonApiClient.getScheduledAppointmentsForPrisonerNumbersAsync(
          rolloutPrison.prisonCode,
          prisonerNumbers,
          date,
          timeSlot,
        )
      }
    }

    val activities = async {
      if (rolloutPrison.activitiesRolledOut) {
        emptyList()
      } else {
        prisonApiClient.getScheduledActivitiesForPrisonerNumbersAsync(
          rolloutPrison.prisonCode,
          prisonerNumbers,
          date,
          timeSlot,
        )
      }
    }

    val courtEvents = async {
      if (!date.isAfter(LocalDate.now())) {
        prisonApiClient.getScheduledCourtEventsForPrisonerNumbersAsync(
          rolloutPrison.prisonCode,
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
        rolloutPrison.prisonCode,
        prisonerNumbers,
        date,
        timeSlot,
      )
    }

    val transfers = async {
      fetchExternalTransfersIfDateIsToday(date, rolloutPrison.prisonCode, prisonerNumbers)
    }

    val adjudications = async {
      adjudicationsHearingAdapter.getAdjudicationHearings(
        agencyId = rolloutPrison.prisonCode,
        date = date,
        prisonerNumbers = prisonerNumbers,
        timeSlot = timeSlot,
        prisonRegime = prisonRegime,
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
    rolloutPrison: RolloutPrisonPlan,
    prisonerNumber: String,
    dateRange: LocalDateRange,
  ): List<PrisonerSchedule> = LocalDate.now()
    .takeIf { dateRange.includes(it) }
    ?.let { today ->
      fetchExternalTransfersIfDateIsToday(today, rolloutPrison.prisonCode, setOf(prisonerNumber))
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
  ): List<PrisonerScheduledActivity> =
    prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
      prisonCode = prisonCode,
      prisonerNumbers = prisonerNumbers,
      date = date,
      timeSlot = slot,
    )

  private fun getMultiplePrisonersAppointments(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): List<AppointmentInstance> {
    val timeRange = timeSlot?.let {
      prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(
        prisonCode = prisonCode,
        timeSlot = it,
        dayOfWeek = date.dayOfWeek,
      )
    }
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

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.AdjudicationsHearingAdapter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.excludeTodayWithoutAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.multiplePrisonerActivitiesToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.multiplePrisonerAppointmentsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.multiplePrisonerCourtEventsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.multiplePrisonerTransfersToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.multiplePrisonerVisitsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.nomisAdjudicationsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformAppointmentInstanceToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformPrisonerScheduledActivityToScheduledEvents
import java.time.LocalDate
import java.time.LocalTime

@Service
class ScheduledEventService(
  private val prisonApiClient: PrisonApiClient,
  private val rolloutPrisonService: RolloutPrisonService,
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val prisonRegimeService: PrisonRegimeService,
  private val adjudicationsHearingAdapter: AdjudicationsHearingAdapter,
  private val locationService: LocationService,
) {
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
    appointmentCategories: Map<String, AppointmentCategory>,
  ): PrisonerScheduledEvents? = runBlocking {
    val eventPriorities = withContext(Dispatchers.IO) { prisonRegimeService.getEventPrioritiesForPrison(prisonCode) }
    val prisonLocations = prisonApiClient.getEventLocationsForPrison(prisonCode)
    val locationsForAppointmentsMap = locationService.getLocationDetailsForAppointmentsMap(prisonCode)

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
            date,
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
            appointmentCategories,
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
  ): List<PrisonerScheduledActivity> = prisonerScheduledActivityRepository.getScheduledActivitiesForPrisonerListAndDate(
    prisonCode = prisonCode,
    prisonerNumbers = prisonerNumbers,
    date = date,
    timeSlot = slot,
  )
    .excludeTodayWithoutAttendance()

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

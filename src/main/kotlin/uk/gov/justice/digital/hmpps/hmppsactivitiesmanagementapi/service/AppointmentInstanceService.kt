package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toPrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toScheduledEvent
import java.time.LocalDate
import java.time.LocalTime

private const val EVENT_CLASS = "INT_MOV"
private const val EVENT_SOURCE = "APP"
private const val EVENT_STATUS = "SCH"
private const val EVENT_TYPE = "APP"
private const val EVENT_TYPE_DESC = "Appointment"

/**
 * Fetches appointment data from either the Prison API or the Appointment Instance Repository, depending on the rollout
 * status of the prison.
 */
@Service
class AppointmentInstanceService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val locationService: LocationService,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val prisonRegimeService: PrisonRegimeService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getScheduledEvents(
    rolloutPrison: RolloutPrison,
    bookingId: Long,
    dateRange: LocalDateRange,
  ): List<ScheduledEvent> {
    return if (rolloutPrison.isAppointmentsEnabled()) {
      log.info("Fetching scheduled events from Appointment Instance Repository for Rollout Prison [${rolloutPrison.rolloutPrisonId}], Booking ID [$bookingId] and Date Range [$dateRange]")
      appointmentInstanceRepository.findByBookingIdAndDateRange(bookingId, dateRange.start, dateRange.endInclusive)
        .toScheduledEvent(EVENT_TYPE, EVENT_TYPE_DESC, EVENT_CLASS, EVENT_STATUS, EVENT_SOURCE)
    } else {
      log.info("Fetching scheduled events from Prison API for Rollout Prison [${rolloutPrison.rolloutPrisonId}], Booking ID [$bookingId] and Date Range [$dateRange]")
      prisonApiClient.getScheduledAppointments(bookingId, dateRange).block() ?: emptyList()
    }
  }

  fun getPrisonerSchedules(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    rolloutPrison: RolloutPrison,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): List<PrisonerSchedule> {
    return if (rolloutPrison.isAppointmentsEnabled()) {
      log.info(
        "Fetching prisoner schedules from Appointment Instance Repository for Rollout Prison [${rolloutPrison.rolloutPrisonId}], Prison Code [$prisonCode], " +
          "Prisoner Numbers [$prisonerNumbers], Date  [$date] and TimeSlot [$timeSlot]",
      )
      val timeRange = timeSlot?.let { prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, it) }
      val earliestStartTime = timeRange?.start ?: LocalTime.of(0, 0)
      val latestStartTime = timeRange?.end ?: LocalTime.of(23, 59)

      val locationMap = locationService.getLocationsForAppointments(prisonCode)!!
        .associateBy { it.locationId }

      val prisonersWithAppointments = appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberAndDateAndTime(
        prisonCode,
        prisonerNumbers,
        date,
        earliestStartTime,
        latestStartTime,
      )

      val prisonerMap = if (prisonersWithAppointments.isNotEmpty()) {
        prisonerSearchApiClient.findByPrisonerNumbers(prisonersWithAppointments.map { it.prisonerNumber }).block()!!
          .associateBy { it.prisonerNumber }
      } else {
        emptyMap()
      }

      prisonersWithAppointments.toPrisonerSchedule(prisonerMap, locationMap, EVENT_TYPE, EVENT_STATUS)
    } else {
      log.info(
        "Fetching prisoner schedules from Prison API for Rollout Prison [${rolloutPrison.rolloutPrisonId}], Prison Code [$prisonCode], " +
          "Prisoner Numbers [$prisonerNumbers], Date  [$date] and TimeSlot [$timeSlot]",
      )
      prisonApiClient.getScheduledAppointmentsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot).block() ?: emptyList()
    }
  }
}

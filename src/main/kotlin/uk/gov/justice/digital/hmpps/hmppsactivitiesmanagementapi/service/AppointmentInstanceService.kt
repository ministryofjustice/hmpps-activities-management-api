package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.map.toPrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.map.toScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentsDataSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import java.time.LocalDate
import java.time.LocalTime

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

  fun getScheduledEvents(rolloutPrison: RolloutPrison, bookingId: Long, dateRange: LocalDateRange): Mono<List<ScheduledEvent>> {
    log.info("Fetching scheduled events for Rollout Prison [$rolloutPrison], Booking ID [$bookingId] and Date Range [$dateRange]")
    return if (shouldUsePrisonApi(rolloutPrison)) {
      log.info("Fetching scheduled events from Prison API")
      prisonApiClient.getScheduledAppointments(bookingId, dateRange)
    } else {
      log.info("Fetching scheduled events from Appointment Instance Repository")
      Mono.just(
        appointmentInstanceRepository.findByBookingIdAndDateRange(bookingId, dateRange.start, dateRange.endInclusive).toScheduledEvent(),
      )
    }
  }

  fun getPrisonerSchedules(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    rolloutPrison: RolloutPrison,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): Mono<List<PrisonerSchedule>> {
    log.info(
      "Fetching prisoner schedules for Rollout Prison [$rolloutPrison], Prison Code [$prisonCode], " +
        "Prisoner Numbers [$prisonerNumbers], Date  [$date] and TimeSlot [$timeSlot]",
    )
    return if (shouldUsePrisonApi(rolloutPrison)) {
      log.info("Fetching prisoner schedules from Prison API")
      prisonApiClient.getScheduledAppointmentsForPrisonerNumbers(prisonCode, prisonerNumbers, date, timeSlot)
    } else {
      log.info("Fetching prisoner schedules from Appointment Instance Repository")
      val timeRange = timeSlot?.let { prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, it) }
      val earliestStartTime = timeRange?.start ?: LocalTime.of(0, 0)
      val latestStartTime = timeRange?.end ?: LocalTime.of(23, 59)

      val locationMap = locationService.getLocationsForAppointments(prisonCode)!!
        .associateBy { it.locationId }

      val prisonersWithAppointments = appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberAndDateAndTime(prisonCode, prisonerNumbers, date, earliestStartTime, latestStartTime)
      log.info("Fetched [${prisonersWithAppointments.size}] prisoners with appointments from the Appointment Instance Repository")

      val prisonerMap = if (prisonersWithAppointments.isNotEmpty()) {
        prisonerSearchApiClient.findByPrisonerNumbers(prisonersWithAppointments.map { it.prisonerNumber }).block()!!
          .associateBy { it.prisonerNumber }
      } else { emptyMap() }

      Mono.just(

        prisonersWithAppointments.toPrisonerSchedule(prisonerMap, locationMap),
      )
    }
  }

  /**
   * Determines whether the data should be fetched from the Prison API or the Appointment Instance Repository
   *
   *
   *  - If the prison is not yet rolled out (rolloutPrison.active == false) then use the Prison API
   *  - If the prison IS rolled out (rolloutPrison.active == true) AND the data source is set to PRISON_API then use
   *  the Prison API
   *  - If the prison IS rolled out (rolloutPrison.active == true) AND the data source is set to ACTIVITIES_SERVICE
   *  then use the Appointment Instance Repository
   *
   *  @return true if the Prison API should be used, false otherwise
   */
  private fun shouldUsePrisonApi(rolloutPrison: RolloutPrison) =
    !rolloutPrison.active || rolloutPrison.appointmentsDataSource == AppointmentsDataSource.PRISON_API
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalTimeRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.time.LocalDate
import java.time.LocalTime

@Service
class InternalLocationService(
  private val appointmentSearchRepository: AppointmentSearchRepository,
  private val appointmentAttendeeSearchRepository: AppointmentAttendeeSearchRepository,
  private val appointmentSearchSpecification: AppointmentSearchSpecification,
  private val prisonApiClient: PrisonApiClient,
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository,
  private val prisonRegimeService: PrisonRegimeService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getInternalLocationEventsSummaries(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?) =
    runBlocking {
      checkCaseloadAccess(prisonCode)

      val timeRange =
        timeSlot?.let { prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, it) } ?: LocalTimeRange(
          LocalTime.of(0, 0),
          LocalTime.of(23, 59),
        )

      val locationActivitiesMap = getLocationActivitiesMap(prisonCode, date, timeRange)
      val locationAppointmentsMap = getLocationAppointmentsMap(prisonCode, date, timeRange)

      val internalLocationIds = locationActivitiesMap.keys.union(locationAppointmentsMap.keys).toSet()

      val internalLocationsMap = getInternalLocationsMapByIds(prisonCode, internalLocationIds)

      internalLocationsMap.map {
        InternalLocationEventsSummary(it.key, it.value.agencyId, it.value.description, it.value.userDescription ?: it.value.description)
      }.toSet()
    }

  suspend fun getInternalLocationsMapByIds(prisonCode: String, internalLocationIds: Set<Long>): Map<Long, Location> {
    val internalLocationsMap = prisonApiClient.getEventLocationsAsync(prisonCode)
      .filter { internalLocationIds.contains(it.locationId) }
      .associateBy { it.locationId }
      .toMutableMap()

    // Try to get any missing location ids via prisonApiClient. If found, they will be inactive
    internalLocationIds.filterNot { internalLocationsMap.containsKey(it) }.forEach {
      log.info("Retrieving inactive internal location with id $it")
      runCatching {
        val location = prisonApiClient.getLocationAsync(it, true)
        internalLocationsMap[location.locationId] = location
      }.onFailure { t ->
        log.warn("Failed to retrieve inactive internal location with id $it", t)
      }
    }

    return internalLocationsMap
  }

  private fun getLocationActivitiesMap(prisonCode: String, date: LocalDate, timeRange: LocalTimeRange): Map<Long, PrisonerScheduledActivity> =
    prisonerScheduledActivityRepository.findByPrisonCodeAndDateAndTime(prisonCode, date, timeRange.start, timeRange.end)
      .filterNot { it.internalLocationId == null }
      .associateBy { it.internalLocationId!!.toLong() }

  private fun getLocationAppointmentsMap(prisonCode: String, date: LocalDate, timeRange: LocalTimeRange): Map<Long, AppointmentSearch> {
    val appointmentsSpec = appointmentSearchSpecification.prisonCodeEquals(prisonCode)
      .and(appointmentSearchSpecification.startDateEquals(date))
      .and(appointmentSearchSpecification.startTimeBetween(timeRange.start, timeRange.end))

    val appointments = appointmentSearchRepository.findAll(appointmentsSpec)
    val attendeeMap = appointmentAttendeeSearchRepository.findByAppointmentIds(appointments.map { it.appointmentId })
      .groupBy { it.appointmentSearch.appointmentId }
    appointments.forEach { it.attendees = attendeeMap[it.appointmentId] ?: emptyList() }

    return appointments.filterNot { it.internalLocationId == null }.associateBy { it.internalLocationId!! }
  }
}

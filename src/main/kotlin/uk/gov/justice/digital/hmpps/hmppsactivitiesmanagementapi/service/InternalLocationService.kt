package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.time.LocalDate

@Service
class InternalLocationService(
  private val appointmentSearchRepository: AppointmentSearchRepository,
  private val appointmentAttendeeSearchRepository: AppointmentAttendeeSearchRepository,
  private val appointmentSearchSpecification: AppointmentSearchSpecification,
  private val prisonApiClient: PrisonApiClient,
  private val prisonRegimeService: PrisonRegimeService,
  private val scheduledInstanceService: ScheduledInstanceService,
) {
  suspend fun getInternalLocationEventsSummaries(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?): Set<InternalLocationEventsSummary> {
    checkCaseloadAccess(prisonCode)

    val locationActivitiesMap = getLocationActivitiesMap(prisonCode, date, timeSlot)
    val locationAppointmentsMap = getLocationAppointmentsMap(prisonCode, date, timeSlot)

    val internalLocationIds = locationActivitiesMap.keys.union(locationAppointmentsMap.keys).toSet()

    val internalLocationsMap = getInternalLocationsMapById(prisonCode, internalLocationIds)

    return internalLocationsMap.map {
      InternalLocationEventsSummary(it.key, prisonCode, it.value.description, it.value.userDescription ?: it.value.description)
    }.toSet()
  }

  suspend fun getInternalLocationsMapById(prisonCode: String, internalLocationIds: Set<Long>): Map<Long, Location> {
    val internalLocationsMap = prisonApiClient.getEventLocationsAsync(prisonCode)
      .filter { internalLocationIds.contains(it.locationId) }
      .associateBy { it.locationId }
      .toMutableMap()

    // Get any missing location ids via prisonApiClient. They will be inactive
    internalLocationIds.filterNot { internalLocationsMap.containsKey(it) }.forEach {
      val location = prisonApiClient.getLocationAsync(it, true)
      internalLocationsMap[location.locationId] = location
    }

    return internalLocationsMap
  }

  private fun getLocationActivitiesMap(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?): Map<Long, ActivityScheduleInstance> =
    scheduledInstanceService.getActivityScheduleInstancesByDateRange(prisonCode, LocalDateRange(date, date), timeSlot)
      .filterNot { it.activitySchedule.internalLocation?.id == null }
      .associateBy { it.activitySchedule.internalLocation!!.id.toLong() }

  private fun getLocationAppointmentsMap(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?): Map<Long, AppointmentSearch> {
    var appointmentsSpec = appointmentSearchSpecification.prisonCodeEquals(prisonCode)
      .and(appointmentSearchSpecification.startDateEquals(date))

    timeSlot?.apply {
      val timeRange = timeSlot.let { prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, it) }
      appointmentsSpec = appointmentsSpec.and(
        appointmentSearchSpecification.startTimeBetween(
          timeRange.start,
          timeRange.end.minusMinutes(1),
        ),
      )
    }

    val appointments = appointmentSearchRepository.findAll(appointmentsSpec)
    val attendeeMap = appointmentAttendeeSearchRepository.findByAppointmentIds(appointments.map { it.appointmentId })
      .groupBy { it.appointmentSearch.appointmentId }
    appointments.forEach { it.attendees = attendeeMap[it.appointmentId] ?: emptyList() }

    return appointments.filterNot { it.internalLocationId == null }.associateBy { it.internalLocationId!! }
  }
}

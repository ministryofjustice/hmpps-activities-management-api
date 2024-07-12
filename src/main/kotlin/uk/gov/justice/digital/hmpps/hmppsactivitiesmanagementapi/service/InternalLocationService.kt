package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.AdjudicationsHearingAdapter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.LocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalTimeRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.multiplePrisonerVisitsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.nomisAdjudicationsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformAppointmentInstanceToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformPrisonerScheduledActivityToScheduledEvents
import java.time.LocalDate
import java.time.LocalTime

@Service
class InternalLocationService(
  private val appointmentAttendeeSearchRepository: AppointmentAttendeeSearchRepository,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val appointmentSearchRepository: AppointmentSearchRepository,
  private val appointmentSearchSpecification: AppointmentSearchSpecification,
  private val prisonApiClient: PrisonApiClient,
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository,
  private val prisonRegimeService: PrisonRegimeService,
  private val referenceCodeService: ReferenceCodeService,
  private val adjudicationsHearingAdapter: AdjudicationsHearingAdapter,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
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

  fun getInternalLocationEventsSummaries(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?) =
    runBlocking {
      checkCaseloadAccess(prisonCode)

      val timeRange = getTimeRange(prisonCode, timeSlot)
      val locationActivitiesMap = getLocationActivitiesMap(prisonCode, date, timeRange)
      val locationAppointmentsMap = getLocationAppointmentsMap(prisonCode, date, timeRange)
      val locationVisitsMap = getLocationVisitsMap(prisonCode, date, timeSlot)
      val adjudicationHearingsMap = adjudicationsHearingAdapter.getAdjudicationsByLocation(agencyId = prisonCode, date = date, timeSlot = timeSlot)

      val internalLocationIds = locationActivitiesMap.keys
        .union(locationAppointmentsMap.keys)
        .union(locationVisitsMap.keys)
        .union(adjudicationHearingsMap.keys)

      val internalLocationsMap = getInternalLocationsMapByIds(prisonCode, internalLocationIds)

      internalLocationsMap.map {
        InternalLocationEventsSummary(it.key, it.value.agencyId, it.value.description, it.value.userDescription ?: it.value.description)
      }.toSet()
    }

  private fun getLocationActivitiesMap(prisonCode: String, date: LocalDate, timeRange: LocalTimeRange): Map<Long, PrisonerScheduledActivity> =
    prisonerScheduledActivityRepository.findByPrisonCodeAndDateAndTime(prisonCode, date, timeRange.start, timeRange.end)
      .filterNot { it.internalLocationId == null }
      .associateBy { it.internalLocationId!!.toLong() }

  private fun getLocationAppointmentsMap(prisonCode: String, date: LocalDate, timeRange: LocalTimeRange): Map<Long, AppointmentSearch> {
    val appointmentsSpec = appointmentSearchSpecification.prisonCodeEquals(prisonCode)
      .and(appointmentSearchSpecification.startDateEquals(date))
      .and(appointmentSearchSpecification.startTimeBetween(timeRange.start, timeRange.end))

    val appointments = appointmentSearchRepository.findAll(appointmentsSpec).filterNot { it.internalLocationId == null }
    val attendeeMap = appointmentAttendeeSearchRepository.findByAppointmentIds(appointments.map { it.appointmentId })
      .groupBy { it.appointmentSearch.appointmentId }
    appointments.forEach { it.attendees = attendeeMap[it.appointmentId] ?: emptyList() }

    return appointments.associateBy { it.internalLocationId!! }
  }

  /**
   * This function currently calls the Prison API's GET /api/agencies/{agencyId}/eventLocationsBooked endpoint. This
   * returns a list of locations that have at least one activity, appointment or visit event scheduled to take place there.
   * It does not therefore only return locations with visits scheduled and does not return visits data for those locations.
   *
   * Another issue is that the Prison API uses the following fixed time slot system:
   *
   * - AM - 00:00 - 12:00
   * - PM - 12:00 - 17:00
   * - ED - 17:00 - 23:59
   *
   * This is different to the per prison time slot configuration used by this service where the prison's regime specifies
   * the timeslot. This difference means that locations returned by this function may appear to have no events scheduled
   * or in some cases, the location with events in this service's timeslots appear in other timeslots.
   *
   * Once the Visit Someone in Prison service is rolled out to more of the prison estate and/or is capable of being called
   * for prisons not yet using the service, this function should be switched to use it. This function should retrieve all
   * the visits scheduled to take place at the chosen prison on the selected date. They can then be filtered based on the
   * time slots specified by the prison's regime and returned.
   *
   * An alternative would be to call the Prison API's GET /api/schedules/{agencyId}/locations/{locationId}/usage/VISIT
   * endpoint for every location and filtering out locations with no visits booked. This unfortunately would be a costly
   * set of calls.
   *
   * Until then, this function is named correctly but both returns all locations with events not just those with visits and
   * does not return the visits data required to produce valid capacity data.
   */
  private suspend fun getLocationVisitsMap(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?): Map<Long, LocationSummary> =
    prisonApiClient.getEventLocationsBookedAsync(prisonCode, date, timeSlot).associateBy { it.locationId }

  fun getInternalLocationEvents(prisonCode: String, internalLocationIds: Set<Long>, date: LocalDate, timeSlot: TimeSlot?) =
    runBlocking {
      checkCaseloadAccess(prisonCode)

      val referenceCodesForAppointmentsMap =
        referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)
      val internalLocationsMap = getInternalLocationsMapByIds(prisonCode, internalLocationIds)
      val eventPriorities = prisonRegimeService.getEventPrioritiesForPrison(prisonCode)

      val timeRange = getTimeRange(prisonCode, timeSlot)

      val activities = prisonerScheduledActivityRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTime(
        prisonCode,
        internalLocationIds.map { it.toInt() }.toSet(),
        date,
        timeRange.start,
        timeRange.end,
      )

      val appointments = appointmentInstanceRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTime(
        prisonCode,
        internalLocationIds,
        date,
        timeRange.start,
        timeRange.end,
      )

      val visits = internalLocationIds.flatMap {
        prisonApiClient.getScheduledVisitsForLocationAsync(
          prisonCode,
          it,
          date,
          timeSlot,
        )
      }

      val adjudicationHearings = adjudicationsHearingAdapter.getAdjudicationsByLocation(
        agencyId = prisonCode,
        date = date,
        timeSlot = timeSlot,
      ).filter { internalLocationIds.contains(it.key) }.flatMap { it.value }

      val scheduledEventsMap = transformPrisonerScheduledActivityToScheduledEvents(
        prisonCode,
        eventPriorities,
        activities,
      ).union(
        transformAppointmentInstanceToScheduledEvents(
          prisonCode,
          eventPriorities,
          referenceCodesForAppointmentsMap,
          internalLocationsMap,
          appointments,
        ),
      ).union(
        visits.multiplePrisonerVisitsToScheduledEvents(
          prisonCode,
          eventPriorities.getOrDefault(EventType.VISIT),
        ),
      ).union(
        adjudicationHearings.nomisAdjudicationsToScheduledEvents(
          prisonCode = prisonCode,
          priority = eventPriorities.getOrDefault(EventType.ADJUDICATION_HEARING),
          prisonLocations = emptyMap(),
        ),
      )
        .filterNot { it.internalLocationId == null }.groupBy { it.internalLocationId!! }

      internalLocationsMap.map {
        InternalLocationEvents(
          it.key,
          it.value.agencyId,
          it.value.description,
          it.value.userDescription ?: it.value.description,
          scheduledEventsMap[it.key]?.toSet() ?: emptySet(),
        )
      }.toSet()
    }

  private fun getTimeRange(prisonCode: String, timeSlot: TimeSlot?) =
    timeSlot?.let {
      prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, it)
        .let { tr -> LocalTimeRange(tr.start, tr.end.minusMinutes(1)) }
    } ?: LocalTimeRange(
      LocalTime.of(0, 0),
      LocalTime.of(23, 59),
    )
}

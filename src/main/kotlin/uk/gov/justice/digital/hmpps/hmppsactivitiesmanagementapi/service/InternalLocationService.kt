package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.validation.ValidationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.AdjudicationsHearingAdapter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api.LocationsInsidePrisonAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisMappingAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.LocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalTimeRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.excludeTodayWithoutAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService.LocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.multiplePrisonerVisitsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.nomisAdjudicationsToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformAppointmentInstanceToScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformPrisonerScheduledActivityToScheduledEvents
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Service
class InternalLocationService(
  private val appointmentAttendeeSearchRepository: AppointmentAttendeeSearchRepository,
  private val appointmentInstanceRepository: AppointmentInstanceRepository,
  private val appointmentSearchRepository: AppointmentSearchRepository,
  private val appointmentSearchSpecification: AppointmentSearchSpecification,
  private val prisonApiClient: PrisonApiClient,
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository,
  private val prisonRegimeService: PrisonRegimeService,
  private val appointmentCategoryService: AppointmentCategoryService,
  private val adjudicationsHearingAdapter: AdjudicationsHearingAdapter,
  private val nomisMappingAPIClient: NomisMappingAPIClient,
  private val locationsInsidePrisonAPIClient: LocationsInsidePrisonAPIClient,
) {
  suspend fun getInternalLocationsMapByIds(prisonCode: String, dpsLocationIds: Set<UUID>): Map<Long, LocationDetails> = coroutineScope {
    val locationsAsync = async { locationsInsidePrisonAPIClient.getNonResidentialLocations(prisonCode) }
    val mappingsAsync = async { nomisMappingAPIClient.getLocationMappingsByDpsIds(dpsLocationIds) }

    val locationsMap = locationsAsync
      .await()
      .filter { dpsLocationIds.contains(it.id) }
      .associateBy { it.id }

    val mappings = mappingsAsync
      .await()
      .associateBy { it.dpsLocationId }

    locationsMap.map {
      it.value.toLocationDetails(mappings[it.key]!!.nomisLocationId)
    }.toMapByNomisId()
  }

  suspend fun getInternalLocationEventsSummaries(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?) = coroutineScope {
    checkCaseloadAccess(prisonCode)
    val prisonRegime = prisonRegimeService.getPrisonRegimesByDaysOfWeek(agencyId = prisonCode)

    val locationActivitiesMap = getLocationActivitiesMap(prisonCode, date, timeSlot)
    val locationVisitsMap = getLocationVisitsMap(prisonCode, date, timeSlot)
    val adjudicationHearingsMap = adjudicationsHearingAdapter.getAdjudicationsByLocation(
      agencyId = prisonCode,
      date = date,
      timeSlot = timeSlot,
      prisonRegime = prisonRegime,
    )

    val timeRange = getTimeRange(
      prisonCode = prisonCode,
      timeSlot = timeSlot,
      dayOfWeek = date.dayOfWeek,
    )
    val locationAppointmentsMap = getLocationAppointmentsMap(prisonCode, date, timeRange)

    val internalLocationIds = locationActivitiesMap.keys
      .union(locationAppointmentsMap.keys)
      .union(locationVisitsMap.keys)
      .union(adjudicationHearingsMap.keys)

    val dpsLocationsAsync = async { nomisMappingAPIClient.getLocationMappingsByNomisIds(internalLocationIds) }

    val allDpsLocationsAsync = async { locationsInsidePrisonAPIClient.getNonResidentialLocations(prisonCode) }

    val dpsLocationsByDPSIdMap = dpsLocationsAsync.await().associateBy { it.dpsLocationId }

    val allDpsLocations = allDpsLocationsAsync.await().filter { dpsLocationsByDPSIdMap.contains(it.id) }

    allDpsLocations.map {
      InternalLocationEventsSummary(
        dpsLocationsByDPSIdMap[it.id]!!.nomisLocationId,
        it.id,
        it.prisonId,
        it.code,
        it.localName ?: it.code,
      )
    }.toSet()
  }

  private fun getLocationActivitiesMap(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?) = prisonerScheduledActivityRepository.findByPrisonCodeAndDateAndTimeSlot(prisonCode, date, timeSlot)
    .excludeTodayWithoutAttendance()
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
  private suspend fun getLocationVisitsMap(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?): Map<Long, LocationSummary> {
    val allLocations = prisonApiClient.getEventLocationsBookedAsync(prisonCode, date, timeSlot)

    val locationIdsWithVisits = withContext(Dispatchers.IO) {
      allLocations.map {
        async {
          val locationVisits: List<PrisonerSchedule> = prisonApiClient.getScheduledVisitsForLocationAsync(
            prisonCode,
            it.locationId,
            date,
            timeSlot,
          )

          it.locationId to locationVisits.isNotEmpty()
        }
      }.awaitAll()
        .filter { it.second }
        .map { it.first }
        .toSet()
    }

    return allLocations
      .filter { locationIdsWithVisits.contains(it.locationId) }
      .associateBy { it.locationId }
  }

  @Deprecated("Will be removed in favour of getLocationEvents")
  fun getInternalLocationEvents(prisonCode: String, nomisLocationIds: Set<Long>, date: LocalDate, timeSlot: TimeSlot?) = runBlocking {
    checkCaseloadAccess(prisonCode)

    val dpsLocationIds =
      nomisMappingAPIClient.getLocationMappingsByNomisIds(nomisLocationIds).map { it.dpsLocationId }.toSet()

    getLocationEvents(prisonCode, dpsLocationIds, date, timeSlot)
  }

  fun getLocationEvents(prisonCode: String, dpsLocationIds: Set<UUID>, date: LocalDate, timeSlot: TimeSlot?) = runBlocking {
    checkCaseloadAccess(prisonCode)

    val prisonRegime = prisonRegimeService.getPrisonRegimesByDaysOfWeek(agencyId = prisonCode)
    val appointmentCategories = appointmentCategoryService.getAll()
    val locationsMap = getInternalLocationsMapByIds(prisonCode, dpsLocationIds)
    val eventPriorities = prisonRegimeService.getEventPrioritiesForPrison(prisonCode)

    val internalLocationIds = locationsMap.map { it.key }.toSet()

    val activities = prisonerScheduledActivityRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTimeSlot(
      prisonCode,
      internalLocationIds.map { it.toInt() }.toSet(),
      date,
      timeSlot,
    )
      .excludeTodayWithoutAttendance()

    val timeRange = getTimeRange(
      prisonCode = prisonCode,
      timeSlot = timeSlot,
      dayOfWeek = date.dayOfWeek,
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
      prisonRegime = prisonRegime,
    ).filter { internalLocationIds.contains(it.key) }.flatMap { it.value }

    val scheduledEventsMap = transformPrisonerScheduledActivityToScheduledEvents(
      prisonCode,
      eventPriorities,
      activities,
    ).union(
      transformAppointmentInstanceToScheduledEvents(
        prisonCode,
        eventPriorities,
        appointmentCategories,
        locationsMap,
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

    locationsMap.map {
      InternalLocationEvents(
        it.key,
        it.value.dpsLocationId,
        it.value.agencyId,
        it.value.code,
        it.value.description,
        scheduledEventsMap[it.key]?.toSet() ?: emptySet(),
      )
    }.toSet()
  }

  private fun getTimeRange(prisonCode: String, timeSlot: TimeSlot?, dayOfWeek: DayOfWeek) = timeSlot?.let {
    val regime = prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(
      prisonCode = prisonCode,
      timeSlot = it,
      dayOfWeek = dayOfWeek,
    ) ?: throw ValidationException("no regime found for $prisonCode $dayOfWeek")

    regime.let { tr -> LocalTimeRange(tr.start, tr.end.minusMinutes(1)) }
  } ?: LocalTimeRange(
    LocalTime.of(0, 0),
    LocalTime.of(23, 59),
  )
}

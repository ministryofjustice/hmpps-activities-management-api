package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.toResults
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CREATED_BY_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.END_DATE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISONER_NUMBER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.RESULTS_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_DATE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TIME_SLOT_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import java.time.LocalDate
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class AppointmentSearchService(
  private val appointmentSearchRepository: AppointmentSearchRepository,
  private val appointmentAttendeeSearchRepository: AppointmentAttendeeSearchRepository,
  private val appointmentSearchSpecification: AppointmentSearchSpecification,
  private val prisonRegimeService: PrisonRegimeService,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val telemetryClient: TelemetryClient,
) {
  fun searchAppointments(
    prisonCode: String,
    request: AppointmentSearchRequest,
    principal: Principal,
  ): List<AppointmentSearchResult> {
    checkCaseloadAccess(prisonCode)

    val startTime = System.currentTimeMillis()
    var spec = appointmentSearchSpecification.prisonCodeEquals(prisonCode)

    with(request) {
      appointmentType?.apply {
        spec = spec.and { root, _, cb -> cb.equal(root.get<Long>("appointmentType"), appointmentType) }
      }

      spec = if (endDate != null) {
        spec.and(appointmentSearchSpecification.startDateBetween(startDate!!, endDate))
      } else {
        spec.and(appointmentSearchSpecification.startDateEquals(startDate!!))
      }

      val timeSlotSpecs = timeSlots?.map { slot ->
        val timeRange = prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(
          // TODO just use any day for now, given all live prisons have a single regime
          prisonCode = prisonCode, timeSlot = slot, dayOfWeek = LocalDate.now().dayOfWeek,
        )
        // TODO see hard coded defaults, will not be called, but to discuss with Dave B
        appointmentSearchSpecification.startTimeBetween(
          timeRange?.start ?: LocalTime.now(),
          timeRange?.end?.minusMinutes(1) ?: LocalTime.now(),
        )
      } ?: listOf()

      spec = spec.and(timeSlotSpecs.reduceOrNull { acc, spec -> acc.or(spec) }) ?: spec

      categoryCode?.apply {
        spec = spec.and(appointmentSearchSpecification.categoryCodeEquals(categoryCode))
      }

      internalLocationId?.apply {
        spec = spec.and(appointmentSearchSpecification.internalLocationIdEquals(internalLocationId))
      }

      inCell?.apply {
        spec = spec.and(appointmentSearchSpecification.inCellEquals(inCell))
      }

      if (prisonerNumbers?.isEmpty() == false) {
        spec = spec.and(appointmentSearchSpecification.prisonerNumbersIn(prisonerNumbers))
      }

      createdBy?.apply {
        spec = spec.and(appointmentSearchSpecification.createdByEquals(createdBy))
      }
    }

    val results = appointmentSearchRepository.findAll(spec)

    val attendeeMap = appointmentAttendeeSearchRepository.findByAppointmentIds(results.map { it.appointmentId })
      .groupBy { it.appointmentSearch.appointmentId }

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(prisonCode)

    logAppointmentSearchMetric(principal, prisonCode, request, results.size, startTime)

    return results.filter { it.appointmentType == AppointmentType.GROUP || attendeeMap.containsKey(it.appointmentId) }.toResults(attendeeMap, referenceCodeMap, locationMap)
  }

  private fun logAppointmentSearchMetric(principal: Principal, prisonCode: String, request: AppointmentSearchRequest, results: Int, startTimeInMs: Long) {
    val propertiesMap = mapOf(
      USER_PROPERTY_KEY to principal.name,
      PRISON_CODE_PROPERTY_KEY to prisonCode,
      START_DATE_PROPERTY_KEY to (request.startDate?.toString() ?: ""),
      END_DATE_PROPERTY_KEY to (request.endDate?.toString() ?: ""),
      TIME_SLOT_PROPERTY_KEY to (request.timeSlots?.toString() ?: ""),
      CATEGORY_CODE_PROPERTY_KEY to (request.categoryCode ?: ""),
      INTERNAL_LOCATION_ID_PROPERTY_KEY to (request.internalLocationId?.toString() ?: ""),
      PRISONER_NUMBER_PROPERTY_KEY to (request.prisonerNumbers?.joinToString() ?: ""),
      CREATED_BY_PROPERTY_KEY to (request.createdBy ?: ""),
    )

    val metricsMap = mapOf(
      RESULTS_COUNT_METRIC_KEY to results.toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_SEARCH.value, propertiesMap, metricsMap)
  }
}

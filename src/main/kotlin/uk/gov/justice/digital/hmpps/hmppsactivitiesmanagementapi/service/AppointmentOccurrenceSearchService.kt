package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toResults
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentOccurrenceSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CATEGORY_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.END_DATE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.INTERNAL_LOCATION_ID_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_NAME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.RESULTS_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.START_DATE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TIME_SLOT_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.USER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal

@Service
@Transactional(readOnly = true)
class AppointmentOccurrenceSearchService(
  private val appointmentOccurrenceSearchRepository: AppointmentOccurrenceSearchRepository,
  private val appointmentOccurrenceAllocationSearchRepository: AppointmentOccurrenceAllocationSearchRepository,
  private val appointmentOccurrenceSearchSpecification: AppointmentOccurrenceSearchSpecification,
  private val prisonRegimeService: PrisonRegimeService,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val telemetryClient: TelemetryClient,
) {
  fun searchAppointmentOccurrences(
    prisonCode: String,
    request: AppointmentOccurrenceSearchRequest,
    principal: Principal,
  ): List<AppointmentOccurrenceSearchResult> {
    checkCaseloadAccess(prisonCode)

    val startTime = System.currentTimeMillis()
    var spec = appointmentOccurrenceSearchSpecification.prisonCodeEquals(prisonCode)

    with(request) {
      appointmentType?.apply {
        spec = spec.and { root, _, cb -> cb.equal(root.get<Long>("appointmentType"), appointmentType) }
      }

      spec = if (endDate != null) {
        spec.and(appointmentOccurrenceSearchSpecification.startDateBetween(startDate!!, endDate))
      } else {
        spec.and(appointmentOccurrenceSearchSpecification.startDateEquals(startDate!!))
      }

      timeSlot?.apply {
        val timeRange = timeSlot.let { prisonRegimeService.getTimeRangeForPrisonAndTimeSlot(prisonCode, it) }
        spec = spec.and(
          appointmentOccurrenceSearchSpecification.startTimeBetween(
            timeRange.start,
            timeRange.end.minusMinutes(1),
          ),
        )
      }

      categoryCode?.apply {
        spec = spec.and(appointmentOccurrenceSearchSpecification.categoryCodeEquals(categoryCode))
      }

      internalLocationId?.apply {
        spec = spec.and(appointmentOccurrenceSearchSpecification.internalLocationIdEquals(internalLocationId))
      }

      inCell?.apply {
        spec = spec.and(appointmentOccurrenceSearchSpecification.inCellEquals(inCell))
      }

      if (prisonerNumbers?.isEmpty() == false) {
        spec = spec.and(appointmentOccurrenceSearchSpecification.prisonerNumbersIn(prisonerNumbers))
      }

      createdBy?.apply {
        spec = spec.and(appointmentOccurrenceSearchSpecification.createdByEquals(createdBy))
      }
    }

    val results = appointmentOccurrenceSearchRepository.findAll(spec)

    val allocationsMap = appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(results.map { it.appointmentOccurrenceId })
      .groupBy { it.appointmentOccurrenceSearch.appointmentOccurrenceId }

    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)

    val locationMap = locationService.getLocationsForAppointmentsMap(prisonCode)

    logAppointmentSearchMetric(principal, prisonCode, request, results.size, startTime)
    return results.toResults(allocationsMap, referenceCodeMap, locationMap)
  }

  private fun logAppointmentSearchMetric(principal: Principal, prisonCode: String, request: AppointmentOccurrenceSearchRequest, results: Int, startTimeInMs: Long) {
    val propertiesMap = mapOf(
      USER_PROPERTY_KEY to principal.name,
      PRISON_NAME_PROPERTY_KEY to prisonCode,
      START_DATE_PROPERTY_KEY to (request.startDate?.toString() ?: ""),
      END_DATE_PROPERTY_KEY to (request.endDate?.toString() ?: ""),
      TIME_SLOT_PROPERTY_KEY to (request.timeSlot?.toString() ?: ""),
      CATEGORY_CODE_PROPERTY_KEY to (request.categoryCode ?: ""),
      INTERNAL_LOCATION_ID_PROPERTY_KEY to (request.internalLocationId?.toString() ?: ""),
    )

    val metricsMap = mapOf(
      RESULTS_COUNT_METRIC_KEY to results.toDouble(),
      EVENT_TIME_MS_METRIC_KEY to (System.currentTimeMillis() - startTimeInMs).toDouble(),
    )

    telemetryClient.trackEvent(TelemetryEvent.APPOINTMENT_SEARCH.name, propertiesMap, metricsMap)
  }
}

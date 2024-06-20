package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendanceSummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime

private enum class QueryMode {
  ALL,
  BY_CATEGORY_CODE,
  BY_CUSTOM_NAME,
  BY_CATEGORY_CODE_AND_CUSTOM_NAME,
}

@Service
@Transactional
class AppointmentAttendanceService(
  private val appointmentAttendanceSummaryRepository: AppointmentAttendanceSummaryRepository,
  private val appointmentRepository: AppointmentRepository,
  private val referenceCodeService: ReferenceCodeService,
  private val locationService: LocationService,
  private val appointmentAttendeeSearchRepository: AppointmentAttendeeSearchRepository,
) {
  fun getAppointmentAttendanceSummaries(
    prisonCode: String,
    date: LocalDate,
    categoryCode: String? = null,
    customName: String? = null,
  ): List<AppointmentAttendanceSummary> {
    checkCaseloadAccess(prisonCode)

    val summaries = when (
      getQueryMode(
        categoryCode = categoryCode,
        customName = customName,
      )
    ) {
      QueryMode.ALL -> appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDate(prisonCode, date)
      QueryMode.BY_CATEGORY_CODE -> appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDateAndCategoryCode(prisonCode, date, categoryCode!!)
      QueryMode.BY_CUSTOM_NAME -> appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDateAndCustomName(prisonCode, date, customName!!)
      QueryMode.BY_CATEGORY_CODE_AND_CUSTOM_NAME -> appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDateAndCategoryCodeAndCustomName(prisonCode, date, categoryCode!!, customName!!)
    }
    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)
    val locationMap = locationService.getLocationsForAppointmentsMap(prisonCode)
    val attendeeMap = appointmentAttendeeSearchRepository.findByAppointmentIds(summaries.map { it.appointmentId })
      .groupBy { it.appointmentSearch.appointmentId }

    return summaries.toModel(attendeeMap, referenceCodeMap, locationMap)
  }

  fun markAttendance(appointmentId: Long, request: AppointmentAttendanceRequest, principal: Principal): Appointment {
    val attendanceRecordedTime = LocalDateTime.now()
    val attendanceRecordedBy = principal.name

    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    checkCaseloadAccess(appointment.prisonCode)

    appointment.markPrisonerAttendance(request.attendedPrisonNumbers, request.nonAttendedPrisonNumbers, attendanceRecordedTime, attendanceRecordedBy)

    return appointmentRepository.saveAndFlush(appointment).toModel()
  }

  private fun getQueryMode(categoryCode: String?, customName: String?): QueryMode {
    if (categoryCode == null && customName == null) return QueryMode.ALL
    if (categoryCode != null && customName != null) return QueryMode.BY_CATEGORY_CODE_AND_CUSTOM_NAME
    if (categoryCode != null) return QueryMode.BY_CATEGORY_CODE
    return QueryMode.BY_CUSTOM_NAME
  }
}

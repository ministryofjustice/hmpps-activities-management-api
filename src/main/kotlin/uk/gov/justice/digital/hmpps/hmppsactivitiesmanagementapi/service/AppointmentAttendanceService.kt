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
    appointmentName: String? = null,
    customName: String? = null,
  ): List<AppointmentAttendanceSummary> {
    checkCaseloadAccess(prisonCode)

    val summaries = appointmentAttendanceSummaryRepository.findByPrisonCodeAndStartDate(prisonCode, date)
    val referenceCodeMap = referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)
    val locationMap = locationService.getLocationsForAppointmentsMap(prisonCode)
    val attendeeMap = appointmentAttendeeSearchRepository.findByAppointmentIds(summaries.map { it.appointmentId })
      .groupBy { it.appointmentSearch.appointmentId }

    return summaries.filter {
      customName == null || customName == it.customName
    }.toModel(attendeeMap, referenceCodeMap, locationMap, appointmentName)
  }

  fun markAttendance(appointmentId: Long, request: AppointmentAttendanceRequest, principal: Principal): Appointment {
    val attendanceRecordedTime = LocalDateTime.now()
    val attendanceRecordedBy = principal.name

    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    checkCaseloadAccess(appointment.prisonCode)

    appointment.markPrisonerAttendance(request.attendedPrisonNumbers, request.nonAttendedPrisonNumbers, attendanceRecordedTime, attendanceRecordedBy)

    return appointmentRepository.saveAndFlush(appointment).toModel()
  }
}

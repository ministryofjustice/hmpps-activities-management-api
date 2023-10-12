package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import java.security.Principal
import java.time.LocalDateTime

@Service
@Transactional
class AppointmentAttendanceService(
  private val appointmentRepository: AppointmentRepository,
) {
  fun markAttendance(appointmentId: Long, request: AppointmentAttendanceRequest, principal: Principal): Appointment {
    val attendanceRecordedTime = LocalDateTime.now()
    val attendanceRecordedBy = principal.name

    val appointment = appointmentRepository.findOrThrowNotFound(appointmentId)
    checkCaseloadAccess(appointment.prisonCode)

    appointment.markPrisonerAttendance(request.attendedPrisonNumbers, request.nonAttendedPrisonNumbers, attendanceRecordedTime, attendanceRecordedBy)

    return appointmentRepository.saveAndFlush(appointment).toModel()
  }
}

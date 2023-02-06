package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound

@Service
class AppointmentService(
  private val appointmentRepository: AppointmentRepository
) {
  fun getAppointmentById(appointmentId: Long) =
    appointmentRepository.findOrThrowNotFound(appointmentId).toModel()
}

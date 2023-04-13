package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound

@Service
class AppointmentInstanceService(private val appointmentInstanceRepository: AppointmentInstanceRepository) {
  fun getAppointmentInstanceById(appointmentInstanceId: Long) =
    appointmentInstanceRepository.findOrThrowNotFound(appointmentInstanceId).toModel()
}

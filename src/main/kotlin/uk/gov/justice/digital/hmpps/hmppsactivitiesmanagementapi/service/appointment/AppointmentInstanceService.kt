package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess

@Service
class AppointmentInstanceService(private val appointmentInstanceRepository: AppointmentInstanceRepository) {
  fun getAppointmentInstanceById(appointmentInstanceId: Long): AppointmentInstance {
    val appointmentInstance = appointmentInstanceRepository.findOrThrowNotFound(appointmentInstanceId).toModel()
    checkCaseloadAccess(appointmentInstance.prisonCode)

    return appointmentInstance
  }
}

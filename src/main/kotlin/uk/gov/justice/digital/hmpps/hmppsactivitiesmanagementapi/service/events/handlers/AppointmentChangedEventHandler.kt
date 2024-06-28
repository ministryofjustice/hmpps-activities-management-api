package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentAttendeeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentsChangedEvent
import java.time.LocalDateTime

@Component
class AppointmentChangedEventHandler(
  private val appointmentAttendeeService: AppointmentAttendeeService,
) : EventHandler<AppointmentsChangedEvent> {

  override fun handle(event: AppointmentsChangedEvent): Outcome {
    if (event.cancelAppointments()) {
      appointmentAttendeeService.removePrisonerFromFutureAppointments(
        event.prisonCode(),
        event.prisonerNumber(),
        LocalDateTime.now(),
        CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        "APPOINTMENTS_CHANGED_EVENT",
      )
    }
    return Outcome.success()
  }
}

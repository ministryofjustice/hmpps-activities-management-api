package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentAttendeeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentsChangedEvent

@Component
class AppointmentChangedEventHandler(
  private val appointmentAttendeeService: AppointmentAttendeeService,
) : EventHandler<AppointmentsChangedEvent> {

  override fun handle(event: AppointmentsChangedEvent): Outcome {
    if (event.cancelAppointments()) {
      appointmentAttendeeService.cancelFutureOffenderAppointments(event.prisonCode(), event.prisonerNumber())
    }
    return Outcome.success()
  }
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentOccurrenceAllocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentsChangedEvent

@Component
class AppointmentChangedEventHandler(
  private val appointmentOccurrenceAllocationService: AppointmentOccurrenceAllocationService,
) : EventHandler<AppointmentsChangedEvent> {

  override fun handle(event: AppointmentsChangedEvent): Outcome {
    if (event.cancelAppointments()) {
      appointmentOccurrenceAllocationService.cancelFutureOffenderAppointments(event.prisonCode(), event.prisonerNumber())
    }
    return Outcome.success()
  }
}

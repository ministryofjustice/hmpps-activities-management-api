package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentAttendeeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentsChangedEvent

class AppointmentChangedEventHandlerTest {
  private val appointmentAttendeeService: AppointmentAttendeeService = mock()

  private val appointmentChangedEventHandler = AppointmentChangedEventHandler(appointmentAttendeeService)

  @Test
  fun `remove prisoner from future appointments if cancelAppointments flag is set`() {
    val event: AppointmentsChangedEvent = mock()
    val prisonCode = "PVI"
    val prisonerNumber = "12345"

    whenever(event.prisonCode()).thenReturn(prisonCode)
    whenever(event.prisonerNumber()).thenReturn(prisonerNumber)
    whenever(event.cancelAppointments()).thenReturn(true)

    appointmentChangedEventHandler.handle(event)

    verify(appointmentAttendeeService).removePrisonerFromFutureAppointments(
      prisonCode,
      prisonerNumber,
      CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
      "APPOINTMENTS_CHANGED_EVENT",
    )
  }

  @Test
  fun `does not remove prisoner from future appointments if cancelAppointments flag is not set`() {
    val event: AppointmentsChangedEvent = mock()
    val prisonCode = "PVI"
    val prisonerNumber = "12345"

    whenever(event.prisonCode()).thenReturn(prisonCode)
    whenever(event.prisonerNumber()).thenReturn(prisonerNumber)
    whenever(event.cancelAppointments()).thenReturn(false)

    appointmentChangedEventHandler.handle(event)

    verifyNoInteractions(appointmentAttendeeService)
  }
}

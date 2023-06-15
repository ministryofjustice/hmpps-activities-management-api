package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentOccurrenceAllocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentsChangedEvent

class AppointmentChangedEventHandlerTest {

  private val appointmentOccurrenceAllocationService: AppointmentOccurrenceAllocationService = mock()

  private val appointmentChangedEventHandler = AppointmentChangedEventHandler(appointmentOccurrenceAllocationService)

  @Test
  fun `cancels future appointments if cancelAppointments flag is set`() {
    val event: AppointmentsChangedEvent = mock()
    val prisonCode = "PVI"
    val prisonerNumber = "12345"

    whenever(event.prisonCode()).thenReturn(prisonCode)
    whenever(event.prisonerNumber()).thenReturn(prisonerNumber)
    whenever(event.cancelAppointments()).thenReturn(true)

    appointmentChangedEventHandler.handle(event)

    verify(appointmentOccurrenceAllocationService).cancelFutureOffenderAppointments(prisonCode, prisonerNumber)
  }

  @Test
  fun `does not cancel future appointments if cancelAppointments flag is not set`() {
    val event: AppointmentsChangedEvent = mock()
    val prisonCode = "PVI"
    val prisonerNumber = "12345"

    whenever(event.prisonCode()).thenReturn(prisonCode)
    whenever(event.prisonerNumber()).thenReturn(prisonerNumber)
    whenever(event.cancelAppointments()).thenReturn(false)

    appointmentChangedEventHandler.handle(event)

    verifyNoInteractions(appointmentOccurrenceAllocationService)
  }
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AppointmentEntityListenerTest(@Autowired private val listener: AppointmentEntityListener) {

  @MockBean
  private lateinit var outboundEventsService: OutboundEventsService

  private val appointmentSeries = appointmentSeriesEntity(
    prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 457, "C3456DE" to 457),
  )
  private var appointment = appointmentSeries.appointments().first()

  @Test
  fun `appointment instance updated events raised on appointment update`() {
    listener.onUpdate(appointment)

    appointment.attendees().forEach {
      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentAttendeeId)
    }
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `appointment instance cancelled events raised on appointment update when appointment is cancelled`() {
    appointment.cancelledTime = LocalDateTime.now()
    listener.onUpdate(appointment)

    appointment.attendees().forEach {
      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED, it.appointmentAttendeeId)
    }
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `appointment instance deleted events raised on appointment update when appointment is deleted`() {
    appointment.isDeleted = true
    listener.onUpdate(appointment)

    appointment.attendees().forEach {
      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, it.appointmentAttendeeId)
    }
    verifyNoMoreInteractions(outboundEventsService)
  }
}

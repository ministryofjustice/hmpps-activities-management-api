package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDeletedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AppointmentOccurrenceEntityListenerTest(@Autowired private val listener: AppointmentOccurrenceEntityListener) {

  @MockBean
  private lateinit var outboundEventsService: OutboundEventsService

  private val appointmentSeries = appointmentSeriesEntity(
    prisonerNumberToBookingIdMap = mapOf("A1234BC" to 456, "B2345CD" to 457, "C3456DE" to 457),
  )
  private var appointmentOccurrence = appointmentSeries.appointments().first()

  @Test
  fun `appointment instance updated events raised on occurrence update`() {
    listener.onUpdate(appointmentOccurrence)

    appointmentOccurrence.allocations().forEach {
      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, it.appointmentOccurrenceAllocationId)
    }
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `appointment instance cancelled events raised on occurrence update when occurrence is cancelled`() {
    appointmentOccurrence.cancellationReason = appointmentCancelledReason()
    listener.onUpdate(appointmentOccurrence)

    appointmentOccurrence.allocations().forEach {
      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED, it.appointmentOccurrenceAllocationId)
    }
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `appointment instance deleted events raised on occurrence update when occurrence is deleted`() {
    appointmentOccurrence.cancellationReason = appointmentDeletedReason()
    listener.onUpdate(appointmentOccurrence)

    appointmentOccurrence.allocations().forEach {
      verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, it.appointmentOccurrenceAllocationId)
    }
    verifyNoMoreInteractions(outboundEventsService)
  }
}

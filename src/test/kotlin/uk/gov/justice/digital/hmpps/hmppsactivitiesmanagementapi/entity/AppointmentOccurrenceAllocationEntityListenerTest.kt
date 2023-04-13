package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEventsService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AppointmentOccurrenceAllocationEntityListenerTest(@Autowired private val listener: AppointmentOccurrenceAllocationEntityListener) {

  @MockBean
  private lateinit var outboundEventsService: OutboundEventsService

  @Test
  fun `appointment instance created event raised on creation`() {
    val entity = appointmentEntity().occurrences().first().allocations().first()
    listener.onCreate(entity)

    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_CREATED, entity.appointmentOccurrenceAllocationId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `appointment instance updated event raised on update`() {
    val entity = appointmentEntity().occurrences().first().allocations().first()
    listener.onUpdate(entity)

    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, entity.appointmentOccurrenceAllocationId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `appointment instance deleted event raised on deletion`() {
    val entity = appointmentEntity().occurrences().first().allocations().first()
    listener.onDelete(entity)

    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, entity.appointmentOccurrenceAllocationId)
    verifyNoMoreInteractions(outboundEventsService)
  }
}

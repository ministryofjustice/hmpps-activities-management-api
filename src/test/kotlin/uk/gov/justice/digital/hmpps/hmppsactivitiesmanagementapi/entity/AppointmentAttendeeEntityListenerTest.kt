package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonerReleasedAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.tempRemovalByUserAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class AppointmentAttendeeEntityListenerTest(@Autowired private val listener: AppointmentAttendeeEntityListener) {

  @MockBean
  private lateinit var outboundEventsService: OutboundEventsService

  @Test
  fun `appointment instance created event raised on creation`() {
    val entity = appointmentSeriesEntity().appointments().first().attendees().first()
    listener.onCreate(entity)

    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_CREATED, entity.appointmentAttendeeId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `appointment instance updated event not raised on update`() {
    val entity = appointmentSeriesEntity().appointments().first().attendees().first()
    listener.onUpdate(entity)

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `appointment instance cancelled events raised on update when attendee is removed`() {
    val entity = appointmentSeriesEntity().appointments().first().attendees().first().apply {
      this.remove(removalReason = tempRemovalByUserAppointmentAttendeeRemovalReason(), removedBy = "TEST.USER")
    }
    listener.onUpdate(entity)

    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_CANCELLED, entity.appointmentAttendeeId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `appointment instance deleted events raised on update when attendee is deleted`() {
    val entity = appointmentSeriesEntity().appointments().first().attendees().first().apply {
      this.remove(removalReason = prisonerReleasedAppointmentAttendeeRemovalReason(), removedBy = "TEST.USER")
    }
    listener.onUpdate(entity)

    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, entity.appointmentAttendeeId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `appointment instance deleted event raised on deletion`() {
    val entity = appointmentSeriesEntity().appointments().first().attendees().first()
    listener.onDelete(entity)

    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, entity.appointmentAttendeeId)
    verifyNoMoreInteractions(outboundEventsService)
  }
}

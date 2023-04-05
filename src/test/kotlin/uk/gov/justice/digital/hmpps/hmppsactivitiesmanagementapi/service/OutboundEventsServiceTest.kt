package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class OutboundEventsServiceTest {

  private val eventsPublisher: EventsPublisher = mock()
  private val featureSwitches: FeatureSwitches = mock()
  private val outboundEventsService = OutboundEventsService(eventsPublisher, featureSwitches)
  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Test
  fun `activity schedule created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.ACTIVITY_SCHEDULE_CREATED) } doReturn true }

    outboundEventsService.send(OutboundEvent.ACTIVITY_SCHEDULE_CREATED, 1L)

    verify(
      expectedEventType = "activities.activity-schedule.created",
      expectedAdditionalInformation = ScheduleCreatedInformation(1),
      expectedDescription = "A new activity schedule has been created in the activities management service",
    )
  }

  @Test
  fun `prisoner allocated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_ALLOCATED) } doReturn true }

    outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATED, 1L)

    verify(
      expectedEventType = "activities.prisoner.allocated",
      expectedAdditionalInformation = PrisonerAllocatedInformation(1),
      expectedDescription = "A prisoner has been allocated to an activity in the activities management service",
    )
  }

  @Test
  fun `prisoner allocation amended event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_ALLOCATION_AMENDED) } doReturn true }

    outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)

    verify(
      expectedEventType = "activities.prisoner.allocation.amended",
      expectedAdditionalInformation = PrisonerAllocatedInformation(1),
      expectedDescription = "A prisoner allocation has been amended in the activities management service",
    )
  }

  @Test
  fun `appointment instance created event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.APPOINTMENT_INSTANCE_CREATED) } doReturn true }

    outboundEventsService.send(OutboundEvent.APPOINTMENT_INSTANCE_CREATED, 1L)

    verify(
      expectedEventType = "appointments.appointment-instance.created",
      expectedAdditionalInformation = AppointmentInstanceCreatedInformation(1),
      expectedDescription = "A new appointment instance has been created in the activities management service",
    )
  }

  @Test
  fun `events are not published for any outbound event when not enabled`() {
    featureSwitches.stub { on { isEnabled(any<OutboundEvent>(), any()) } doReturn false }

    OutboundEvent.values().forEach { outboundEventsService.send(it, 1L) }

    verifyNoInteractions(eventsPublisher)
  }

  private fun verify(
    expectedEventType: String,
    expectedAdditionalInformation: AdditionalInformation,
    expectedOccurredAt: LocalDateTime = LocalDateTime.now(),
    expectedDescription: String,
  ) {
    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo(expectedEventType)
      assertThat(additionalInformation).isEqualTo(expectedAdditionalInformation)
      assertThat(occurredAt).isCloseTo(expectedOccurredAt, within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo(expectedDescription)
    }

    verifyNoMoreInteractions(eventsPublisher)
  }
}

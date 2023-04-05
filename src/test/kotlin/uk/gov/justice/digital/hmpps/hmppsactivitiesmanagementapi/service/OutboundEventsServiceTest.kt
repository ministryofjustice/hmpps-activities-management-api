package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.created")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A new activity schedule has been created in the activities management service")
    }
  }

  @Test
  fun `prisoner allocated event with id 1 is sent to the events publisher`() {
    featureSwitches.stub { on { isEnabled(OutboundEvent.PRISONER_ALLOCATED) } doReturn true }

    outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATED, 1L)

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.prisoner.allocated")
      assertThat(additionalInformation).isEqualTo(PrisonerAllocatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A prisoner has been allocated to an activity in the activities management service")
    }
  }

  @Test
  fun `events are not published for any outbound event when not enabled`() {
    OutboundEvent.values().forEach { outboundEventsService.send(it, 1L) }

    verifyNoInteractions(eventsPublisher)
  }
}

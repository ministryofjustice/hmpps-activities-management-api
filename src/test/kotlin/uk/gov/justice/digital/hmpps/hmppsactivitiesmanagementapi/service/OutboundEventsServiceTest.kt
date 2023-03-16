package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class OutboundEventsServiceTest {

  private val eventsPublisher: EventsPublisher = mock()
  private val outboundEventsService = OutboundEventsService(eventsPublisher)
  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Test
  fun `activity schedule created event with id 1 is sent to the events publisher`() {
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
    outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATED, 1L)

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.prisoner.allocated")
      assertThat(additionalInformation).isEqualTo(PrisonerAllocatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A prisoner has been allocated to an activity in the activities management service")
    }
  }
}

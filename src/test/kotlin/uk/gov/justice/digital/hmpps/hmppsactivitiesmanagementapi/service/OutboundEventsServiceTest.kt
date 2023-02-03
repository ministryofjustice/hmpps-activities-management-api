package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDateTime

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
      assertThat(occurredAt).isEqualToIgnoringSeconds(LocalDateTime.now())
      assertThat(description).isEqualTo("A new activity schedule has been created in the activities management service")
    }
  }

  @Test
  fun `activity schedule created event with id 99 is sent to the events publisher`() {
    outboundEventsService.send(OutboundEvent.ACTIVITY_SCHEDULE_CREATED, 99L)

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.created")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(99))
      assertThat(occurredAt).isEqualToIgnoringSeconds(LocalDateTime.now())
      assertThat(description).isEqualTo("A new activity schedule has been created in the activities management service")
    }
  }
}

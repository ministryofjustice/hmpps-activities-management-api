package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventPriority
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventPriorityRepository

class PrisonRegimeServiceTest {

  private val eventPriorityRepository: EventPriorityRepository = mock()
  private val service = PrisonRegimeService(eventPriorityRepository)

  @Test
  fun `default priorities are returned when no priorities for prison`() {
    whenever(eventPriorityRepository.findByPrisonCode("PVI")).thenReturn(emptyList())

    assertThat(service.getEventPrioritiesForPrison("PVI")).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        EventType.COURT_HEARING to listOf(Priority(1)),
        EventType.VISIT to listOf(Priority(2)),
        EventType.ADJUDICATION_HEARING to listOf(Priority(3)),
        EventType.APPOINTMENT to listOf(Priority(4)),
        EventType.ACTIVITY to listOf(Priority(5)),
      )
    )

    verify(eventPriorityRepository).findByPrisonCode("PVI")
  }

  @Test
  fun `existing prison priorities are returned for prison, overriding defaults`() {
    whenever(eventPriorityRepository.findByPrisonCode("MDI")).thenReturn(
      listOf(
        EventPriority(1, "MDI", EventType.ACTIVITY, 1),
        EventPriority(2, "MDI", EventType.APPOINTMENT, 2),
        EventPriority(3, "MDI", EventType.VISIT, 3),
        EventPriority(4, "MDI", EventType.ADJUDICATION_HEARING, 4),
        EventPriority(5, "MDI", EventType.COURT_HEARING, 5),
      )
    )

    assertThat(service.getEventPrioritiesForPrison("MDI")).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        EventType.ACTIVITY to listOf(Priority(1)),
        EventType.APPOINTMENT to listOf(Priority(2)),
        EventType.VISIT to listOf(Priority(3)),
        EventType.ADJUDICATION_HEARING to listOf(Priority(4)),
        EventType.COURT_HEARING to listOf(Priority(5)),
      )
    )

    verify(eventPriorityRepository).findByPrisonCode("MDI")
  }
}

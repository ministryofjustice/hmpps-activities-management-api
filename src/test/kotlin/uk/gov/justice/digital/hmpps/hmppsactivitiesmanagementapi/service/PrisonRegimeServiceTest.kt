package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType

class PrisonRegimeServiceTest {

  private val service = PrisonRegimeService()

  @Test
  fun `default priorities`() {
    assertThat(service.getEventPrioritiesForPrison("PVI")).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        EventType.COURT_HEARING to setOf(Priority(1)),
        EventType.VISIT to setOf(Priority(2)),
        EventType.ADJUDICATION_HEARING to setOf(Priority(3)),
        EventType.APPOINTMENT to setOf(Priority(4)),
        EventType.ACTIVITY to setOf(Priority(5)),
      )
    )
  }
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PrisonerRejectedFromWaitingListEventTest {

  @Test
  fun `returns correct type`() {
    val event = PrisonerRejectedFromWaitingListEvent(
      1,
      "Some Activity",
      "PBI",
      "Terry",
      "Jones",
      LocalDateTime.now(),
      "Bob",
    )
    assertThat(event.type()).isEqualTo(AuditEventType.PRISONER_REJECTED_FROM_WAITING_LIST)
  }

  @Test
  fun `returns correct string representation`() {
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    val event = PrisonerRejectedFromWaitingListEvent(
      1,
      "Some Activity",
      "A123456",
      "Terry",
      "Jones",
      createdAt,
      "Bob",
    )
    val expectedToString = "Prisoner A123456 Jones, Terry was rejected from the waiting list for activity 'Some Activity'(1). " +
      "Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }
}

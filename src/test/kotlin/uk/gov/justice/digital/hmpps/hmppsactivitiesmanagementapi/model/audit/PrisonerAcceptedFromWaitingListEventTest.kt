package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PrisonerAcceptedFromWaitingListEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    val event = PrisonerAcceptedFromWaitingListEvent(
      1,
      "Some Activity",
      "PBI",
      "AA12346",
      LocalDateTime.now(),

    )
    assertThat(event.auditEventType).isEqualTo(AuditEventType.PRISONER_ACCEPTED_FROM_WAITING_LIST)
  }

  @Test
  fun `returns correct string representation`() {
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    val event = PrisonerAcceptedFromWaitingListEvent(
      1,
      "Some Activity",
      "PBI",
      "AA12346",
      createdAt,

    )
    val expectedToString = "Prisoner AA12346 was accepted onto activity 'Some Activity'(1) from the " +
      "waiting list. Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }
}

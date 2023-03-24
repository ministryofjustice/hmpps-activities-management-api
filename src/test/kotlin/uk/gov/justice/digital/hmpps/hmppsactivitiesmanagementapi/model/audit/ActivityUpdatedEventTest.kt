package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityUpdatedEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    val event = ActivityUpdatedEvent(1, "Some Activity", "PBI", "C", LocalDate.now(), LocalDateTime.now())
    assertThat(event.auditEventType).isEqualTo(AuditEventType.ACTIVITY_UPDATED)
  }

  @Test
  fun `returns correct string representation`() {
    val startDate = LocalDate.of(2023, 3, 23)
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    val event = ActivityUpdatedEvent(
      1,
      "Some Activity",
      "PBI",
      "C",
      startDate,
      createdAt,
    )
    val expectedToString = "An activity called 'Some Activity'(1) with category C and starting on 2023-03-23 " +
      "at prison PBI was updated. Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }
}

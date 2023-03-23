package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityCreatedEventTest {

  @Test
  fun `returns correct type`() {
    val event = ActivityCreatedEvent(1, "Some Activity", "PBI", "C", LocalDate.now(), LocalDateTime.now(), "Bob")
    assertThat(event.type()).isEqualTo(AuditEventType.ACTIVITY_CREATED)
  }

  @Test
  fun `returns correct string representation`() {
    val startDate = LocalDate.of(2023, 3, 23)
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    val event = ActivityCreatedEvent(
      1,
      "Some Activity",
      "PBI",
      "C",
      startDate,
      createdAt,
      "Bob",
    )
    val expectedToString = "An activity called 'Some Activity' with category C and starting on 2023-03-23 " +
      "at prison PBI. Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }
}

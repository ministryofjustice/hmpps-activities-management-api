package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PrisonerAllocatedEventTest {

  @Test
  fun `returns correct type`() {
    val event = PrisonerAllocatedEvent(
      1,
      "Some Activity",
      "PBI",
      "Terry",
      "Jones",
      42,
      LocalDate.now(),
      LocalTime.now(),
      LocalTime.now(),
      LocalDateTime.now(),
      "Bob",
    )
    assertThat(event.type()).isEqualTo(AuditEventType.PRISONER_ALLOCATED)
  }

  @Test
  fun `returns correct string representation`() {
    val startDate = LocalDate.of(2023, 3, 23)
    val startTime = LocalTime.of(9, 0)
    val endTime = LocalTime.of(10, 0)
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    val event = PrisonerAllocatedEvent(
      1,
      "Some Activity",
      "A123456",
      "Terry",
      "Jones",
      42,
      startDate,
      startTime,
      endTime,
      createdAt,
      "Bob",
    )
    val expectedToString = "Prisoner A123456 Jones, Terry was allocated to activity 'Some Activity'(1) " +
      "scheduled on 2023-03-23 between 09:00 and 10:00 (scheduleId = 42). Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import java.time.LocalDate
import java.time.LocalDateTime

class ActivityUpdatedEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    val event = createEvent()
    assertThat(event.auditEventType).isEqualTo(AuditEventType.ACTIVITY_UPDATED)
  }

  @Test
  fun `returns correct string representation`() {
    val event = createEvent()
    val expectedToString = "An activity called 'Some Activity'(1) with category C and starting on 2023-03-23 " +
      "at prison PBI was updated. Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }

  @Test
  fun `returns the correct json representation`() {
    val event = createEvent()
    val expectedJson = """{"activityId":1,"activityName":"Some Activity","prisonCode":"PBI","createdAt":"2023-03-22T09:00:03","createdBy":"Bob"}"""
    assertThat(event.toJson()).isEqualTo(expectedJson)
  }

  @Test
  fun `returns the correct LocalAuditRecord representation`() {
    val event = createEvent()
    val expectedLocalAuditRecord = LocalAuditRecord(
      username = "Bob",
      auditType = AuditType.ACTIVITY,
      detailType = AuditEventType.ACTIVITY_UPDATED,
      recordedTime = LocalDateTime.of(2023, 3, 22, 9, 0, 3),
      prisonCode = "PBI",
      activityId = 1,
      message = "An activity called 'Some Activity'(1) with category C and starting on 2023-03-23 " +
        "at prison PBI was updated. Event created on 2023-03-22 at 09:00:03 by Bob.",
    )

    assertThat(event.toLocalAuditRecord()).isEqualTo(expectedLocalAuditRecord)
  }

  private fun createEvent(): ActivityUpdatedEvent {
    val startDate = LocalDate.of(2023, 3, 23)
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    return ActivityUpdatedEvent(
      1,
      "Some Activity",
      "PBI",
      "C",
      startDate,
      createdAt,
    )
  }
}

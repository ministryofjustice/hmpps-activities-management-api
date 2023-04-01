package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PrisonerAllocatedEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    val event = createEvent()
    assertThat(event.auditEventType).isEqualTo(AuditEventType.PRISONER_ALLOCATED)
  }

  @Test
  fun `returns correct string representation`() {
    val event = createEvent()
    val expectedToString = "Prisoner AA12346 was allocated to activity 'Some Activity'(1) " +
      "scheduled on 2023-03-23 between 09:00 and 10:00 (scheduleId = 42). Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }

  @Test
  fun `returns the correct json representation`() {
    val event = createEvent()
    val expectedJson = """{"activityId":1,"activityName":"Some Activity","prisonCode":"PBI","prisonerNumber":"AA12346","scheduleId":42,"date":"2023-03-23","startTime":"09:00:00","endTime":"10:00:00","createdAt":"2023-03-22T09:00:03","createdBy":"Bob"}"""
    assertThat(event.toJson()).isEqualTo(expectedJson)
  }

  @Test
  fun `returns the correct LocalAuditRecord representation`() {
    val event = createEvent()
    val expectedLocalAuditRecord = LocalAuditRecord(
      username = "Bob",
      auditType = AuditType.PRISONER,
      detailType = AuditEventType.PRISONER_ALLOCATED,
      recordedTime = LocalDateTime.of(2023, 3, 22, 9, 0, 3),
      prisonCode = "PBI",
      prisonerNumber = "AA12346",
      activityId = 1,
      activityScheduleId = 42,
      message = "Prisoner AA12346 was allocated to activity 'Some Activity'(1) " +
        "scheduled on 2023-03-23 between 09:00 and 10:00 (scheduleId = 42). Event created on 2023-03-22 at 09:00:03 by Bob.",
    )

    assertThat(event.toLocalAuditRecord()).isEqualTo(expectedLocalAuditRecord)
  }

  private fun createEvent(): PrisonerAllocatedEvent {
    val startDate = LocalDate.of(2023, 3, 23)
    val startTime = LocalTime.of(9, 0)
    val endTime = LocalTime.of(10, 0)
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    return PrisonerAllocatedEvent(
      1,
      "Some Activity",
      "PBI",
      "AA12346",
      42,
      startDate,
      startTime,
      endTime,
      createdAt,

    )
  }
}

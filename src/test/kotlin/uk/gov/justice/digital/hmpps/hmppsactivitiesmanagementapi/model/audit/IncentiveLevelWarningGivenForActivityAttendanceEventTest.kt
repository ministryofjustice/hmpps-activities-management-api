package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class IncentiveLevelWarningGivenForActivityAttendanceEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    val event = createEvent()
    assertThat(event.auditEventType).isEqualTo(AuditEventType.INCENTIVE_LEVEL_WARNING_GIVEN_FOR_ACTIVITY_ATTENDANCE)
  }

  @Test
  fun `returns correct string representation`() {
    val event = createEvent()
    val expectedToString = "An incentive level warning was given to prisoner AA12346 for activity 'Some Activity'(1) " +
      "scheduled on 2023-03-23 between 09:00 and 10:00 (scheduleId = 42). Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }

  @Test
  fun `returns the correct json representation`() {
    val event = createEvent()
    val expectedJson = """{"activityId":1,"activityName":"Some Activity","prisonCode":"PBI","prisonerNumber":"AA12346","scheduleId":42,"date":"2023-03-23","startTime":"09:00:00","endTime":"10:00:00","createdAt":"2023-03-22T09:00:03","createdBy":"Bob"}"""
    assertThat(event.toJson()).isEqualTo(expectedJson)
  }

  private fun createEvent(): IncentiveLevelWarningGivenForActivityAttendanceEvent {
    val startDate = LocalDate.of(2023, 3, 23)
    val startTime = LocalTime.of(9, 0)
    val endTime = LocalTime.of(10, 0)
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    return IncentiveLevelWarningGivenForActivityAttendanceEvent(
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

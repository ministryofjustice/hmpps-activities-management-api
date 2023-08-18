package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import java.time.LocalDateTime

class PrisonerDeclinedFromWaitingListEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    assertThat(createEvent().auditEventType).isEqualTo(AuditEventType.PRISONER_DECLINED_FROM_WAITING_LIST)
  }

  @Test
  fun `returns correct string representation`() {
    val expectedToString = "Prisoner AA12346 was declined from the waiting list (1) for activity 'Some Activity'(1). " +
      "Event created on 2023-03-22 at 09:00:03 by Test."
    assertThat(createEvent().toString()).isEqualTo(expectedToString)
  }

  @Test
  fun `returns the correct json representation`() {
    val expectedJson =
      """{"activityId":1,"activityName":"Some Activity","prisonCode":"PBI","prisonerNumber":"AA12346","scheduleId":1,"createdAt":"2023-03-22T09:00:03","createdBy":"Test"}"""
    assertThat(createEvent().toJson()).isEqualTo(expectedJson)
  }

  @Test
  fun `returns the correct LocalAuditRecord representation`() {
    val expectedLocalAuditRecord = LocalAuditRecord(
      username = "Test",
      auditType = AuditType.PRISONER,
      detailType = AuditEventType.PRISONER_DECLINED_FROM_WAITING_LIST,
      recordedTime = LocalDateTime.of(2023, 3, 22, 9, 0, 3),
      prisonCode = "PBI",
      prisonerNumber = "AA12346",
      activityId = 1,
      activityScheduleId = 1,
      message = "Prisoner AA12346 was declined from the waiting list (1) for activity 'Some Activity'(1). " +
        "Event created on 2023-03-22 at 09:00:03 by Test.",
    )

    assertThat(createEvent().toLocalAuditRecord()).isEqualTo(expectedLocalAuditRecord)
  }

  private fun createEvent(): PrisonerDeclinedFromWaitingListEvent {
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    return PrisonerDeclinedFromWaitingListEvent(
      waitingListId = 1,
      activityId = 1,
      scheduleId = 1,
      activityName = "Some Activity",
      prisonCode = "PBI",
      prisonerNumber = "AA12346",
      declinedAt = createdAt,
      declinedBy = "Test",
    )
  }
}

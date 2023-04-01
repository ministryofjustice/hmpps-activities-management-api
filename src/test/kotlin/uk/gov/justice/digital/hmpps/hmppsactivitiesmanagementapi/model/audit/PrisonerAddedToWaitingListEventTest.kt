package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PrisonerAddedToWaitingListEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    val event = createEvent()
    assertThat(event.auditEventType).isEqualTo(AuditEventType.PRISONER_ADDED_TO_WAITING_LIST)
  }

  @Test
  fun `returns correct string representation`() {
    val event = createEvent()
    val expectedToString = "Prisoner AA12346 was added to the waiting list for activity 'Some Activity'(1). " +
      "Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }

  @Test
  fun `returns the correct json representation`() {
    val event = createEvent()
    val expectedJson = """{"activityId":1,"activityName":"Some Activity","prisonCode":"PBI","prisonerNumber":"AA12346","createdAt":"2023-03-22T09:00:03","createdBy":"Bob"}"""
    assertThat(event.toJson()).isEqualTo(expectedJson)
  }

  private fun createEvent(): PrisonerAddedToWaitingListEvent {
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    return PrisonerAddedToWaitingListEvent(
      1,
      "Some Activity",
      "PBI",
      "AA12346",
      createdAt,
    )
  }
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import java.time.LocalDateTime

class AppointmentDeletedEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    val event = createEvent()
    assertThat(event.auditEventType).isEqualTo(AuditEventType.APPOINTMENT_DELETED)
  }

  @Test
  fun `returns correct string representation`() {
    val event = createEvent()
    val expectedToString = "An appointment with ID '1' and Occurrence ID '2' at prison PBI was deleted. Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }

  @Test
  fun `returns the correct json representation`() {
    val event = createEvent()
    val expectedJson =
      """{"appointmentId":1,"appointmentOccurrenceId":2,"prisonCode":"PBI","applyTo":"ALL_FUTURE_OCCURRENCES","createdAt":"2023-03-22T09:00:03","createdBy":"Bob"}"""
    assertThat(event.toJson()).isEqualTo(expectedJson)
  }

  private fun createEvent(): AppointmentDeletedEvent {
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    return AppointmentDeletedEvent(
      appointmentId = 1,
      appointmentOccurrenceId = 2,
      prisonCode = "PBI",
      applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES,
      createdAt = createdAt,
    )
  }
}
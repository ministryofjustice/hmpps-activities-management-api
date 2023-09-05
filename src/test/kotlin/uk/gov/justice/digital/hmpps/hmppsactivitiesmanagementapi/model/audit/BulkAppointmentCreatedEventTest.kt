package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BulkAppointmentCreatedEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    val event = createEvent()
    assertThat(event.auditEventType).isEqualTo(AuditEventType.BULK_APPOINTMENT_CREATED)
  }

  @Test
  fun `returns correct string representation`() {
    val event = createEvent()
    val expectedToString = "A bulk appointment with ID '1' with category C and starting on 2023-03-23 at prison PBI was created. Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }

  @Test
  fun `returns the correct json representation`() {
    val event = createEvent()
    val expectedJson =
      """{"bulkAppointmentId":1,"prisonCode":"PBI","categoryCode":"C","hasDescription":true,"internalLocationId":2,"startDate":"2023-03-23","prisonerNumbers":["123456"],"createdAt":"2023-03-22T09:00:03","createdBy":"Bob"}"""
    assertThat(event.toJson()).isEqualTo(expectedJson)
  }

  private fun createEvent(): BulkAppointmentCreatedEvent {
    val startDate = LocalDate.of(2023, 3, 23)
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    return BulkAppointmentCreatedEvent(
      bulkAppointmentId = 1,
      prisonCode = "PBI",
      categoryCode = "C",
      hasDescription = true,
      internalLocationId = 2,
      startDate = startDate,
      prisonerNumbers = listOf("123456"),
      createdAt = createdAt,
    )
  }
}
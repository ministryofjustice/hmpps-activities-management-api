package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AppointmentOccurrenceEditedEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    val event = createEvent()
    assertThat(event.auditEventType).isEqualTo(AuditEventType.APPOINTMENT_OCCURRENCE_EDITED)
  }

  @Test
  fun `returns correct string representation`() {
    val event = createEvent()
    val expectedToString = "An appointment with ID '1' with category C and starting on 2023-03-23 at prison PBI was edited. Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }

  @Test
  fun `returns the correct json representation`() {
    val event = createEvent()
    val expectedJson =
      """{"appointmentId":1,"appointmentOccurrenceId":2,"prisonCode":"PBI","originalCategoryCode":"A","categoryCode":"C","originalInternalLocationId":3,"internalLocationId":2,"originalStartDate":"2023-03-22","startDate":"2023-03-23","originalStartTime":"08:00:00","startTime":"09:00:00","originalEndTime":"10:15:00","endTime":"10:30:00","applyTo":"ALL_FUTURE_OCCURRENCES","createdAt":"2023-03-22T09:00:03","createdBy":"Bob"}"""
    assertThat(event.toJson()).isEqualTo(expectedJson)
  }

  private fun createEvent(): AppointmentOccurrenceEditedEvent {
    val originalStartDate = LocalDate.of(2023, 3, 22)
    val startDate = LocalDate.of(2023, 3, 23)
    val originalStartTime = LocalTime.of(8, 0)
    val startTime = LocalTime.of(9, 0)
    val originalEndTime = LocalTime.of(10, 15)
    val endTime = LocalTime.of(10, 30)
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    return AppointmentOccurrenceEditedEvent(
      appointmentId = 1,
      appointmentOccurrenceId = 2,
      prisonCode = "PBI",
      originalCategoryCode = "A",
      categoryCode = "C",
      originalInternalLocationId = 3,
      internalLocationId = 2,
      originalStartDate = originalStartDate,
      startDate = startDate,
      originalStartTime = originalStartTime,
      startTime = startTime,
      originalEndTime = originalEndTime,
      endTime = endTime,
      applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES,
      createdAt = createdAt,
    )
  }
}

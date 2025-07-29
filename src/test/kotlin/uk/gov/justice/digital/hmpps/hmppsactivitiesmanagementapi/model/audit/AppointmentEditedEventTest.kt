package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class AppointmentEditedEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    val event = createEvent()
    assertThat(event.auditEventType).isEqualTo(AuditEventType.APPOINTMENT_EDITED)
  }

  @Test
  fun `returns correct string representation`() {
    val event = createEvent()
    val expectedToString = "An appointment with id '2' from series id '1' with category C and starting on 2023-03-23 at prison PBI was edited. Event created on 2023-03-22 at 09:00:03 by Bob."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }

  @Test
  fun `returns the correct json representation`() {
    val event = createEvent()
    val expectedJson =
      """
        {
          "appointmentSeriesId": 1,
          "appointmentId": 2,
          "prisonCode": "PBI",
          "originalCategoryCode": "A",
          "categoryCode": "C",
          "originalTierCode": "TIER_1",
          "tierCode": "TIER_2",
          "originalOrganiserCode": "PRISONER",
          "organiserCode": "PRISON_STAFF",
          "originalInternalLocationId": 3,
          "internalLocationId": 2,
          "dpsLocationId": "44444444-1111-2222-3333-444444444444",
          "originalStartDate": "2023-03-22",
          "startDate": "2023-03-23",
          "originalStartTime": "08:00:00",
          "startTime": "09:00:00",
          "originalEndTime": "10:15:00",
          "endTime": "10:30:00",
          "applyTo": "ALL_FUTURE_APPOINTMENTS",
          "createdTime": "2023-03-22T09:00:03",
          "createdBy": "Bob"
        }
      """

    JSONAssert.assertEquals(expectedJson, event.toJson(), true)
  }

  private fun createEvent(): AppointmentEditedEvent {
    val originalStartDate = LocalDate.of(2023, 3, 22)
    val startDate = LocalDate.of(2023, 3, 23)
    val originalStartTime = LocalTime.of(8, 0)
    val startTime = LocalTime.of(9, 0)
    val originalEndTime = LocalTime.of(10, 15)
    val endTime = LocalTime.of(10, 30)
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    return AppointmentEditedEvent(
      appointmentSeriesId = 1,
      appointmentId = 2,
      prisonCode = "PBI",
      originalCategoryCode = "A",
      categoryCode = "C",
      originalTierCode = "TIER_1",
      tierCode = "TIER_2",
      originalOrganiserCode = "PRISONER",
      organiserCode = "PRISON_STAFF",
      originalInternalLocationId = 3,
      internalLocationId = 2,
      dpsLocationId = UUID.fromString("44444444-1111-2222-3333-444444444444"),
      originalStartDate = originalStartDate,
      startDate = startDate,
      originalStartTime = originalStartTime,
      startTime = startTime,
      originalEndTime = originalEndTime,
      endTime = endTime,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
      createdAt = createdAt,
    )
  }
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AppointmentSetCreatedEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    val event = createEvent()
    assertThat(event.auditEventType).isEqualTo(AuditEventType.APPOINTMENT_SET_CREATED)
  }

  @Test
  fun `returns correct string representation`() {
    val event = createEvent()
    val expectedToString = "An appointment set with id '1' with category C and starting on 2023-03-23 at prison PBI was created. Event created on 2023-03-22 at 09:00:03 by CREATED_BY_USER."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }

  @Test
  fun `returns the correct json representation`() {
    val event = createEvent()
    val expectedJson =
      """
        {
          "appointmentSetId": 1,
          "prisonCode": "PBI",
          "categoryCode": "C",
          "hasCustomName": true,
          "internalLocationId": 2,
          "dpsLocationId": "44444444-1111-2222-3333-444444444444",
          "startDate": "2023-03-23",
          "prisonerNumbers": ["123456"],
          "createdTime": "2023-03-22T09:00:03",
          "createdBy": "CREATED_BY_USER"
        }
      """

    JSONAssert.assertEquals(expectedJson, event.toJson(), true)
  }

  private fun createEvent(): AppointmentSetCreatedEvent {
    val startDate = LocalDate.of(2023, 3, 23)
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    val createdBy = "CREATED_BY_USER"
    return AppointmentSetCreatedEvent(
      appointmentSetId = 1,
      prisonCode = "PBI",
      categoryCode = "C",
      hasCustomName = true,
      internalLocationId = 2,
      dpsLocationId = UUID.fromString("44444444-1111-2222-3333-444444444444"),
      startDate = startDate,
      prisonerNumbers = listOf("123456"),
      createdAt = createdAt,
      createdBy = createdBy,
    )
  }
}

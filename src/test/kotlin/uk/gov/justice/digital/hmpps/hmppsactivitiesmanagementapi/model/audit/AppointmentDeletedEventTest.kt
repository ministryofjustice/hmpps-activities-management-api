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
    val expectedToString = "An appointment with id '2' from series id '1' at prison PBI was deleted. Event created on 2023-03-22 at 09:00:03 by DELETED_BY_USER."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }

  @Test
  fun `returns the correct json representation`() {
    val event = createEvent()
    val expectedJson =
      """{"appointmentSeriesId":1,"appointmentId":2,"prisonCode":"PBI","applyTo":"ALL_FUTURE_APPOINTMENTS","createdTime":"2023-03-22T09:00:03","createdBy":"DELETED_BY_USER"}"""
    assertThat(event.toJson()).isEqualTo(expectedJson)
  }

  private fun createEvent(): AppointmentDeletedEvent {
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    return AppointmentDeletedEvent(
      appointmentSeriesId = 1,
      appointmentId = 2,
      prisonCode = "PBI",
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
      createdAt = createdAt,
      createdBy = "DELETED_BY_USER",
    )
  }
}

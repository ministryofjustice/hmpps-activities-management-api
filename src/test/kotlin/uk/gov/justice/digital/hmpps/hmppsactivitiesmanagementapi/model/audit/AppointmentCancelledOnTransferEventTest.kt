package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AppointmentCancelledOnTransferEventTest : AuditableEventTestBase() {

  @Test
  fun `returns correct type`() {
    val event = createEvent()
    assertThat(event.auditEventType).isEqualTo(AuditEventType.APPOINTMENT_CANCELLED_ON_TRANSFER)
  }

  @Test
  fun `returns correct string representation`() {
    val event = createEvent()
    val expectedToString = "An appointment with id '2' from series id '1' at prison PBI was cancelled on transfer of prisoner 123456. Event created on 2023-03-22 at 09:00:03 by cancelled-on-transfer-event."
    assertThat(event.toString()).isEqualTo(expectedToString)
  }

  @Test
  fun `returns the correct json representation`() {
    val event = createEvent()
    val expectedJson =
      """{"appointmentSeriesId":1,"appointmentId":2,"prisonCode":"PBI","prisonerNumbers":["123456"],"createdTime":"2023-03-22T09:00:03","createdBy":"cancelled-on-transfer-event"}"""
    assertThat(event.toJson()).isEqualTo(expectedJson)
  }

  private fun createEvent(): AppointmentCancelledOnTransferEvent {
    val createdAt = LocalDateTime.of(2023, 3, 22, 9, 0, 3)
    return AppointmentCancelledOnTransferEvent(
      appointmentSeriesId = 1,
      appointmentId = 2,
      prisonCode = "PBI",
      prisonerNumber = "123456",
      createdAt = createdAt,
    )
  }
}

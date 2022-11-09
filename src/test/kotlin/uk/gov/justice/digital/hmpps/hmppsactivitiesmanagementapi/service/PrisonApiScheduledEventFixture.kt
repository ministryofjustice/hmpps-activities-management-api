package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

class PrisonApiScheduledEventFixture {
  companion object {
    fun instance(
      bookingId: Long = 900001,
      eventClass: String = "INT_MOV",
      eventStatus: String = "SCH",
      eventType: String = "APP",
      eventTypeDesc: String = "Appointment",
      eventSubType: String = "GOVE",
      eventSubTypeDesc: String = "Govenor",
      eventDate: LocalDate = LocalDate.of(2022, 11, 8),
      eventLocation: String = "INDUCTION CLASSROOM",
      eventSource: String = "APP",
      eventSourceCode: String = "APP",
      eventSourceDesc: String = "Dont be late",
      startTime: String = "2022-11-08T17:00:00",
      endTime: String = "2022-11-08T18:00:00",
    ): PrisonApiScheduledEvent = PrisonApiScheduledEvent(
      bookingId = bookingId,
      eventClass = eventClass,
      eventStatus = eventStatus,
      eventType = eventType,
      eventTypeDesc = eventTypeDesc,
      eventSubType = eventSubType,
      eventSubTypeDesc = eventSubTypeDesc,
      eventDate = eventDate,
      eventLocation = eventLocation,
      eventSource = eventSource,
      eventSourceCode = eventSourceCode,
      eventSourceDesc = eventSourceDesc,
      startTime = startTime,
      endTime = endTime,
    )
  }
}

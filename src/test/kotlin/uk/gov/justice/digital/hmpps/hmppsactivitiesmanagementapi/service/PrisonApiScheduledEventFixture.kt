package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

object PrisonApiScheduledEventFixture {
  fun appointmentInstance(
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

  fun visitInstance(
    bookingId: Long = 900002,
    eventClass: String = "INT_MOV",
    eventStatus: String = "SCH",
    eventType: String = "VISIT",
    eventTypeDesc: String = "Visit",
    eventSubType: String = "VISIT",
    eventSubTypeDesc: String = "Visits",
    eventDate: LocalDate = LocalDate.of(2022, 11, 12),
    eventLocation: String = "SOCIAL VISTS",
    eventSource: String = "VIS",
    eventSourceCode: String = "SCON",
    eventSourceDesc: String = "Social Contact",
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

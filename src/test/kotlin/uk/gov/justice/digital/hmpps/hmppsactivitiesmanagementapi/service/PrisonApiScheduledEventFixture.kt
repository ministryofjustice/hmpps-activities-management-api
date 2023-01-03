package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import java.math.BigDecimal
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

object PrisonApiScheduledEventFixture {
  fun appointmentInstance(
    agencyId: String = "MDI",
    bookingId: Long = 900001,
    eventClass: String = "INT_MOV",
    eventId: Long = 1,
    eventStatus: String = "SCH",
    eventType: String = "APP",
    eventTypeDesc: String = "Appointment",
    eventSubType: String = "GOVE",
    eventSubTypeDesc: String = "Governor",
    eventDate: LocalDate = LocalDate.of(2022, 12, 14),
    eventLocationId: Long = 1,
    eventLocation: String = "GOVERNORS OFFICE",
    eventSource: String = "APP",
    eventSourceCode: String = "APP",
    eventSourceDesc: String = "Dont be late",
    startTime: String = "2022-12-14T17:00:00",
    endTime: String = "2022-12-14T18:00:00",
  ): PrisonApiScheduledEvent = PrisonApiScheduledEvent(
    agencyId = agencyId,
    bookingId = bookingId,
    eventId = eventId,
    eventClass = eventClass,
    eventStatus = eventStatus,
    eventType = eventType,
    eventTypeDesc = eventTypeDesc,
    eventSubType = eventSubType,
    eventSubTypeDesc = eventSubTypeDesc,
    eventDate = eventDate,
    eventLocationId = eventLocationId,
    eventLocation = eventLocation,
    eventSource = eventSource,
    eventSourceCode = eventSourceCode,
    eventSourceDesc = eventSourceDesc,
    startTime = startTime,
    endTime = endTime,
  )

  fun activityInstance(
    agencyId: String = "MDI",
    bookingId: Long = 900001,
    eventClass: String = "INT_MOV",
    eventId: Long = 1,
    eventStatus: String = "SCH",
    eventType: String = "PRISON_ACT",
    eventTypeDesc: String = "Prison Activities",
    eventSubType: String = "PA",
    eventSubTypeDesc: String = "Prison Activities",
    eventDate: LocalDate = LocalDate.of(2022, 12, 14),
    eventLocation: String = "WORKSHOP 10 - BRICKS",
    eventSource: String = "PA",
    eventSourceCode: String = "BRICK-PM",
    eventSourceDesc: String = "Bricks PM",
    paid: Boolean = false,
    payRate: BigDecimal = BigDecimal.valueOf(1.05),
    locationCode: String = "WS10",
    startTime: String = "2022-12-14T13:15:00",
    endTime: String = "2022-12-14T16:15:00",
  ) = PrisonApiScheduledEvent(
    agencyId = agencyId,
    bookingId = bookingId,
    eventClass = eventClass,
    eventId = eventId,
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
    paid = paid,
    payRate = payRate,
    locationCode = locationCode,
    startTime = startTime,
    endTime = endTime,
  )

  fun visitInstance(
    agencyId: String = "MDI",
    bookingId: Long = 900001,
    eventClass: String = "INT_MOV",
    eventId: Long = 1,
    eventStatus: String = "SCH",
    eventType: String = "VISIT",
    eventTypeDesc: String = "Visit",
    eventSubType: String = "VISIT",
    eventSubTypeDesc: String = "Visits",
    eventDate: LocalDate = LocalDate.of(2022, 12, 14),
    eventLocationId: Long = 1,
    eventLocation: String = "VISITS ROOM",
    eventSource: String = "VIS",
    eventSourceCode: String = "SCON",
    eventSourceDesc: String = "Social Contact",
    startTime: String = "2022-11-08T17:00:00",
    endTime: String = "2022-11-08T18:00:00",
  ): PrisonApiScheduledEvent = PrisonApiScheduledEvent(
    agencyId = agencyId,
    bookingId = bookingId,
    eventClass = eventClass,
    eventId = eventId,
    eventStatus = eventStatus,
    eventType = eventType,
    eventTypeDesc = eventTypeDesc,
    eventSubType = eventSubType,
    eventSubTypeDesc = eventSubTypeDesc,
    eventDate = eventDate,
    eventLocationId = eventLocationId,
    eventLocation = eventLocation,
    eventSource = eventSource,
    eventSourceCode = eventSourceCode,
    eventSourceDesc = eventSourceDesc,
    startTime = startTime,
    endTime = endTime,
  )
}

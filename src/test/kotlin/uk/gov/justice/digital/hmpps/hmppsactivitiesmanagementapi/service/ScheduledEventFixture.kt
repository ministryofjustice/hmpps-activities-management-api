package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import java.time.LocalDate
import java.time.LocalTime

object ScheduledEventFixture {
  fun instance(
    prisonCode: String = "MDI",
    eventId: Long = 10001,
    bookingId: Long = 900001,
    location: String = "INDUCTION CLASSROOM",
    locationId: Long = 101,
    eventClass: String = "INT_MOV",
    eventStatus: String = "SCH",
    eventType: String = "APP",
    eventTypeDesc: String = "Appointment",
    details: String = "Dont be late",
    prisonerNumber: String = "GF10001",
    date: LocalDate = LocalDate.of(2022, 10, 1),
    startTime: LocalTime = LocalTime.of(12, 0, 0),
    endTime: LocalTime = LocalTime.of(13, 0, 0),
  ) = ScheduledEvent(
    prisonCode = prisonCode,
    eventId = eventId,
    bookingId = bookingId,
    location = location,
    locationId = locationId,
    eventClass = eventClass,
    eventStatus = eventStatus,
    eventType = eventType,
    eventTypeDesc = eventTypeDesc,
    details = details,
    prisonerNumber = prisonerNumber,
    date = date,
    startTime = startTime,
    endTime = endTime,
  )
}

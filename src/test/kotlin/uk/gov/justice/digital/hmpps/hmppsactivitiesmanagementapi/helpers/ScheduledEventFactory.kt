package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val startDate = LocalDate.of(2022, 12, 21)
private val startTime = LocalTime.of(10, 0)
private val endTime = LocalTime.of(11, 0)

fun scheduledEvents() = listOf(
  ScheduledEvent(
    bookingId = 900001L,
    startTime = LocalDateTime.of(startDate, startTime).format(DateTimeFormatter.ISO_DATE_TIME),
    endTime = LocalDateTime.of(startDate, endTime).format(DateTimeFormatter.ISO_DATE_TIME),
    eventType = "APP",
    eventTypeDesc = "Appointment",
    eventClass = "INT_MOV",
    eventId = 123,
    eventStatus = "SCH",
    eventDate = startDate,
    eventSource = "APP",
    eventSubType = "456",
    eventSubTypeDesc = "Category Desc",
    agencyId = "PBI",
  ),
)

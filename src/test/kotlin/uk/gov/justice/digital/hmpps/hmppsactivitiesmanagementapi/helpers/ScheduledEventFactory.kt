package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val startDate = LocalDate.of(2022, 12, 21)
private val startTime = LocalTime.of(10, 0)
private val endTime = LocalTime.of(11, 0)

fun scheduledEvents() = listOf(
  ScheduledEvent(
    bookingId = 900001L,
    startTime = LocalDateTime.of(startDate, startTime).toIsoDateTime(),
    endTime = LocalDateTime.of(startDate, endTime).toIsoDateTime(),
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

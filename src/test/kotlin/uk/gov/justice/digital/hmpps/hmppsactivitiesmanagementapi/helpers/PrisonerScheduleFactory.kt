package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val startDate = LocalDate.of(2022, 12, 21)
private val startTime = LocalTime.of(10, 0)
private val endTime = LocalTime.of(11, 0)

fun prisonerSchedules() = listOf(
  PrisonerSchedule(
    cellLocation = "A1",
    comment = "PS Comment",
    event = "Category Code",
    eventDescription = "Category Description",
    eventLocation = "Location Description",
    eventStatus = "SCH",
    eventType = "APP",
    firstName = "John",
    lastName = "Smith",
    locationId = 123,
    offenderNo = "456",
    startTime = LocalDateTime.of(startDate, startTime).toIsoDateTime(),
    endTime = LocalDateTime.of(startDate, endTime).toIsoDateTime(),
  ),
)

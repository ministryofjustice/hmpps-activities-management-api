package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.map

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val EVENT_CLASS = "INT_MOV"
private const val EVENT_SOURCE = "APP"
private const val EVENT_STATUS = "SCH"
private const val EVENT_TYPE = "APP"
private const val EVENT_TYPE_DESC = "Appointment"

/**
 * Maps a List<AppointmentInstance> to a List<PrisonerSchedule>
 *
 *   @param prisonerLookup Map of prisonerNumber -> Prisoner to facilitate data lookup
 *   @param locationLookup Map of locationId -> Location to facilitate data lookup
 */
fun List<AppointmentInstance>.toPrisonerSchedule(
  prisonerLookup: Map<String, Prisoner>,
  locationLookup: Map<Long, Location>,
) = map {
  PrisonerSchedule(
    cellLocation = prisonerLookup[it.prisonerNumber]?.cellLocation!!,
    comment = it.comment,
    event = it.category.code,
    eventDescription = it.category.description,
    eventLocation = locationLookup[it.internalLocationId]?.userDescription,
    eventStatus = EVENT_STATUS,
    eventType = EVENT_TYPE,
    firstName = prisonerLookup[it.prisonerNumber]?.firstName!!,
    lastName = prisonerLookup[it.prisonerNumber]?.lastName!!,
    locationId = it.internalLocationId,
    offenderNo = it.prisonerNumber,
    startTime = LocalDateTime.of(it.appointmentDate, it.startTime).format(DateTimeFormatter.ISO_DATE_TIME),
    endTime = it.endTime?.let { _ -> LocalDateTime.of(it.appointmentDate, it.endTime).format(DateTimeFormatter.ISO_DATE_TIME) },
  )
}

/**
 * Maps List<AppointmentInstance> to List<ScheduledEvent>
 */
fun List<AppointmentInstance>.toScheduledEvent() = map {
  ScheduledEvent(
    bookingId = it.bookingId,
    startTime = LocalDateTime.of(it.appointmentDate, it.startTime).format(DateTimeFormatter.ISO_DATE_TIME),
    endTime = it.endTime?.let { _ -> LocalDateTime.of(it.appointmentDate, it.endTime).format(DateTimeFormatter.ISO_DATE_TIME) },
    eventType = EVENT_TYPE,
    eventTypeDesc = EVENT_TYPE_DESC,
    eventClass = EVENT_CLASS,
    eventId = it.appointmentInstanceId,
    eventStatus = EVENT_STATUS,
    eventDate = it.appointmentDate,
    eventSource = EVENT_SOURCE,
    eventSubType = it.category.code,
    eventSubTypeDesc = it.category.description,
    agencyId = it.prisonCode,
  )
}

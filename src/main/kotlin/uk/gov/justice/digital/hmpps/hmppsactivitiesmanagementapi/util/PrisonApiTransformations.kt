package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.Priority
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent as ModelScheduleEvent

/**
 * Transform functions providing a thin layer to transform prison api types into their API model equivalents and vice-versa.
 */

fun List<PrisonerSchedule>.prisonApiAppointmentsToScheduledEvents(
  prisonCode: String,
  priorities: List<Priority>?,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.APPOINTMENT, priorities)

fun List<PrisonerSchedule>.prisonApiCourtEventsToScheduledEvents(
  prisonCode: String,
  priorities: List<Priority>?,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.COURT_HEARING, priorities)

fun List<PrisonerSchedule>.prisonApiVisitsToScheduledEvents(
  prisonCode: String,
  priorities: List<Priority>?,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.VISIT, priorities)

fun List<PrisonerSchedule>.prisonApiActivitiesToScheduledEvents(
  prisonCode: String,
  priorities: List<Priority>?,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.ACTIVITY, priorities)

fun List<PrisonerSchedule>.prisonApiTransfersToScheduledEvents(
  prisonCode: String,
  priorities: List<Priority>?,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.EXTERNAL_TRANSFER, priorities)

private fun List<PrisonerSchedule>.prisonApiPrisonerScheduleToScheduledEvents(
  prisonCode: String,
  eventType: EventType?,
  priorities: List<Priority>?,
) = map {
  ScheduledEvent(
    prisonCode = prisonCode,
    eventId = it.eventId,
    bookingId = it.bookingId,
    locationId = it.locationId,
    location = it.eventLocation ?: "External", // Don't show the real court location
    eventClass = it.event,
    eventStatus = it.eventStatus,
    eventType = eventType?.name ?: it.eventType,
    eventTypeDesc = eventType?.name ?: it.eventType,
    event = it.event,
    eventDesc = it.eventDescription,
    details = it.comment ?: it.eventDescription,
    prisonerNumber = it.offenderNo,
    date = LocalDateTime.parse(it.startTime).toLocalDate(),
    startTime = LocalDateTime.parse(it.startTime).toLocalTime(),
    endTime = it.endTime?.let { endTime -> LocalDateTime.parse(endTime).toLocalTime() },
    priority = priorities?.let { pList -> getPriority(it.eventType, pList) }
      ?: eventType?.defaultPriority,
  )
}

fun CourtHearings.prisonApiCourtHearingsToScheduledEvents(
  bookingId: Long,
  prisonCode: String?,
  prisonerNumber: String?,
  eventType: String?,
  defaultPriority: Int?,
  priorities: List<Priority>?,
) = this.hearings?.map {
  ScheduledEvent(
    prisonCode = prisonCode,
    eventId = it.id,
    bookingId = bookingId,
    locationId = null,
    location = it.location?.description,
    eventClass = null,
    eventStatus = null,
    eventType = eventType,
    eventTypeDesc = null,
    event = null,
    eventDesc = null,
    details = null,
    prisonerNumber = prisonerNumber,
    date = LocalDateTime.parse(it.dateTime).toLocalDate(),
    startTime = LocalDateTime.parse(it.dateTime).toLocalTime(),
    endTime = null,
    priority = priorities?.let { pList -> getPriority(null, pList) }
      ?: defaultPriority,
  )
}

fun List<OffenderAdjudicationHearing>.prisonApiOffenderAdjudicationsToScheduledEvents(
  prisonCode: String,
  priorities: List<Priority>?,
): List<ModelScheduleEvent> =
  map { it.toScheduledEvent(prisonCode, priorities) }

fun OffenderAdjudicationHearing.toScheduledEvent(
  prisonCode: String,
  priorities: List<Priority>?,
) = let {
  ScheduledEvent(
    prisonCode = prisonCode,
    eventId = it.hearingId,
    bookingId = null,
    locationId = it.internalLocationId,
    location = it.internalLocationDescription,
    eventClass = null,
    eventStatus = it.eventStatus,
    eventType = EventType.ADJUDICATION_HEARING.name,
    eventTypeDesc = null,
    event = null,
    eventDesc = null,
    details = null,
    prisonerNumber = it.offenderNo,
    date = LocalDateTime.parse(it.startTime).toLocalDate(),
    startTime = LocalDateTime.parse(it.startTime).toLocalTime(),
    endTime = null, // TODO consider defaulting a fixed number of hours as adjudications have no concept of end time.
    priority = priorities?.let { pList -> getPriority(null, pList) } ?: EventType.ADJUDICATION_HEARING.defaultPriority,
  )
}

fun List<uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent>.prisonApiScheduledEventToScheduledEvents(
  prisonerNumber: String?,
  eventType: String?,
  defaultPriority: Int?,
  priorities: List<Priority>?,
) = map {
  ScheduledEvent(
    prisonCode = it.agencyId,
    eventId = it.eventId,
    bookingId = it.bookingId,
    locationId = it.eventLocationId,
    location = it.eventLocation,
    eventClass = it.eventClass,
    eventStatus = it.eventStatus,
    eventType = eventType ?: it.eventType,
    eventTypeDesc = it.eventTypeDesc,
    event = it.eventSubType,
    eventDesc = it.eventSubTypeDesc,
    details = it.eventSourceDesc,
    prisonerNumber = prisonerNumber,
    date = it.eventDate,
    startTime = LocalDateTime.parse(it.startTime).toLocalTime(),
    endTime = it.endTime?.let { endTime -> LocalDateTime.parse(endTime).toLocalTime() },
    priority = priorities?.let { pList -> getPriority(it.eventSubType, pList) }
      ?: defaultPriority,
  )
}

// TODO this does NOT reflect the change in activity categories ...

private fun getPriority(category: String?, priorities: List<Priority>): Int? =
  priorities.fold(listOf<Priority>()) { acc, next ->
    if (next.eventCategory == null && acc.isEmpty()) {
      listOf(next)
    } else {
      when (next.eventCategory) {
        EventCategory.EDUCATION -> if (category?.startsWith("EDU") == true) listOf(next) else acc
        EventCategory.GYM_SPORTS_FITNESS -> if (category?.startsWith("GYM") == true) listOf(next) else acc
        EventCategory.INDUCTION -> if (category == "IND" || category == "INDUC") listOf(next) else acc
        EventCategory.INDUSTRIES -> if (category == "LACO") listOf(next) else acc
        EventCategory.INTERVENTIONS -> if (category == "INTERV") listOf(next) else acc
        EventCategory.LEISURE_SOCIAL -> if (category == "LEI") listOf(next) else acc
        EventCategory.SERVICES -> if (category == "SERV") listOf(next) else acc
        else -> {
          acc
        }
      }
    }
  }.firstOrNull()?.priority

fun ReferenceCode?.toAppointmentCategorySummary(code: String) =
  if (this == null) {
    AppointmentCategorySummary(code, "UNKNOWN")
  } else {
    AppointmentCategorySummary(this.code, this.description)
  }

fun List<ReferenceCode>.toAppointmentCategorySummary() = map { it.toAppointmentCategorySummary(it.code) }

fun Location?.toAppointmentLocationSummary(locationId: Long, prisonCode: String) =
  if (this == null) {
    AppointmentLocationSummary(locationId, prisonCode, "UNKNOWN")
  } else {
    AppointmentLocationSummary(this.locationId, this.agencyId, this.userDescription ?: this.description)
  }

fun UserDetail?.toSummary(username: String) =
  if (this == null) {
    UserSummary(-1, username, "UNKNOWN", "UNKNOWN")
  } else {
    UserSummary(this.staffId, this.username, this.firstName, this.lastName)
  }

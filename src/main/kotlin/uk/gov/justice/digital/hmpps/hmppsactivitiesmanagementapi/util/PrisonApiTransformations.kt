package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.Priority
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent as ModelScheduleEvent

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

// TODO this does reflect the change in activity categories ...

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

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.Priority
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings as PrisonApiCourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location as PrisonApiLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing as PrisonApiOffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule as PrisonApiPrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ReferenceCode as PrisonApiReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.UserDetail as PrisonApiUserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary as ModelAppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary as ModelAppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent as ModelScheduleEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary as ModelUserSummary

/**
 * Transform functions providing a thin layer to transform prison api types into their API model equivalents and vice-versa.
 */

const val ADJUDICATION_HEARING_DURATION_TWO_HOURS = 2L

fun List<PrisonApiPrisonerSchedule>.prisonApiAppointmentsToScheduledEvents(
  prisonCode: String,
  priorities: List<Priority>?,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.APPOINTMENT, priorities)

fun List<PrisonApiPrisonerSchedule>.prisonApiCourtEventsToScheduledEvents(
  prisonCode: String,
  priorities: List<Priority>?,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.COURT_HEARING, priorities)

fun List<PrisonApiPrisonerSchedule>.prisonApiVisitsToScheduledEvents(
  prisonCode: String,
  priorities: List<Priority>?,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.VISIT, priorities)

fun List<PrisonApiPrisonerSchedule>.prisonApiActivitiesToScheduledEvents(
  prisonCode: String,
  priorities: List<Priority>?,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.ACTIVITY, priorities)

fun List<PrisonApiPrisonerSchedule>.prisonApiTransfersToScheduledEvents(
  prisonCode: String,
  priorities: List<Priority>?,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.EXTERNAL_TRANSFER, priorities)

private fun List<PrisonApiPrisonerSchedule>.prisonApiPrisonerScheduleToScheduledEvents(
  prisonCode: String,
  eventType: EventType?,
  priorities: List<Priority>?,
) = map {
  ModelScheduleEvent(
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

fun PrisonApiCourtHearings.prisonApiCourtHearingsToScheduledEvents(
  bookingId: Long,
  prisonCode: String?,
  prisonerNumber: String?,
  eventType: String?,
  defaultPriority: Int?,
  priorities: List<Priority>?,
) = this.hearings?.map {
  ModelScheduleEvent(
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

fun List<PrisonApiOffenderAdjudicationHearing>.prisonApiOffenderAdjudicationsToScheduledEvents(
  prisonCode: String,
  priorities: List<Priority>?,
): List<ModelScheduleEvent> =
  map { it.toScheduledEvent(prisonCode, priorities) }

fun PrisonApiOffenderAdjudicationHearing.toScheduledEvent(
  prisonCode: String,
  priorities: List<Priority>?,
) = let {
  ModelScheduleEvent(
    prisonCode = prisonCode,
    eventId = it.hearingId,
    bookingId = null,
    locationId = it.internalLocationId,
    location = it.internalLocationDescription,
    eventClass = null,
    eventStatus = it.eventStatus,
    eventType = EventType.ADJUDICATION_HEARING.name,
    eventTypeDesc = it.hearingType,
    event = null,
    eventDesc = null,
    details = null,
    prisonerNumber = it.offenderNo,
    date = LocalDateTime.parse(it.startTime).toLocalDate(),
    startTime = LocalDateTime.parse(it.startTime).toLocalTime(),
    endTime = LocalDateTime.parse(it.startTime).toLocalTime().plusHours(ADJUDICATION_HEARING_DURATION_TWO_HOURS),
    priority = priorities?.let { pList -> getPriority(null, pList) } ?: EventType.ADJUDICATION_HEARING.defaultPriority,
  )
}

fun List<PrisonApiScheduledEvent>.prisonApiScheduledEventToScheduledEvents(
  prisonerNumber: String?,
  eventType: String?,
  defaultPriority: Int?,
  priorities: List<Priority>?,
) = map {
  ModelScheduleEvent(
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

fun PrisonApiReferenceCode?.toAppointmentCategorySummary(code: String) =
  if (this == null) {
    ModelAppointmentCategorySummary(code, "UNKNOWN")
  } else {
    ModelAppointmentCategorySummary(this.code, this.description)
  }

fun List<PrisonApiReferenceCode>.toAppointmentCategorySummary() = map { it.toAppointmentCategorySummary(it.code) }

fun PrisonApiLocation?.toAppointmentLocationSummary(locationId: Long, prisonCode: String) =
  if (this == null) {
    ModelAppointmentLocationSummary(locationId, prisonCode, "UNKNOWN")
  } else {
    ModelAppointmentLocationSummary(this.locationId, this.agencyId, this.userDescription ?: this.description)
  }

fun PrisonApiUserDetail?.toSummary(username: String) =
  if (this == null) {
    ModelUserSummary(-1, username, "UNKNOWN", "UNKNOWN")
  } else {
    ModelUserSummary(this.staffId, this.username, this.firstName, this.lastName)
  }

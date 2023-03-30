package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
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

fun List<PrisonApiPrisonerSchedule>.prisonApiPrisonAppointmentsToScheduledEvents(
  prisonCode: String,
  priority: Int,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.APPOINTMENT, priority)

fun List<PrisonApiPrisonerSchedule>.prisonApiPrisonCourtEventsToScheduledEvents(
  prisonCode: String,
  priority: Int,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.COURT_HEARING, priority)

fun List<PrisonApiPrisonerSchedule>.prisonApiPrisonVisitsToScheduledEvents(
  prisonCode: String,
  priority: Int,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.VISIT, priority)

fun List<PrisonApiPrisonerSchedule>.prisonApiPrisonActivitiesToScheduledEvents(
  prisonCode: String,
  priority: Int,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.ACTIVITY, priority)

fun List<PrisonApiPrisonerSchedule>.prisonApiPrisonTransfersToScheduledEvents(
  prisonCode: String,
  priority: Int,
) = prisonApiPrisonerScheduleToScheduledEvents(prisonCode, EventType.EXTERNAL_TRANSFER, priority)

private fun List<PrisonApiPrisonerSchedule>.prisonApiPrisonerScheduleToScheduledEvents(
  prisonCode: String,
  eventType: EventType,
  priority: Int,
) = map {
  ModelScheduleEvent(
    prisonCode = prisonCode,
    eventId = it.eventId,
    bookingId = it.bookingId,
    locationId = it.locationId,
    location = it.eventLocation ?: "External", // Don't show the real court location
    eventClass = it.event,
    eventStatus = it.eventStatus,
    eventType = eventType.name,
    eventTypeDesc = eventType.name,
    event = it.event,
    eventDesc = it.eventDescription,
    details = it.comment ?: it.eventDescription,
    prisonerNumber = it.offenderNo,
    date = LocalDateTime.parse(it.startTime).toLocalDate(),
    startTime = LocalDateTime.parse(it.startTime).toLocalTime(),
    endTime = it.endTime?.let { endTime -> LocalDateTime.parse(endTime).toLocalTime() },
    priority = priority,
  )
}

fun PrisonApiCourtHearings.prisonApiCourtHearingsToScheduledEvents(
  bookingId: Long,
  prisonCode: String?,
  prisonerNumber: String?,
  priority: Int,
) = this.hearings?.map {
  ModelScheduleEvent(
    prisonCode = prisonCode,
    eventId = it.id,
    bookingId = bookingId,
    locationId = null,
    location = it.location?.description,
    eventClass = null,
    eventStatus = null,
    eventType = EventType.COURT_HEARING.name,
    eventTypeDesc = null,
    event = null,
    eventDesc = null,
    details = null,
    prisonerNumber = prisonerNumber,
    date = LocalDateTime.parse(it.dateTime).toLocalDate(),
    startTime = LocalDateTime.parse(it.dateTime).toLocalTime(),
    endTime = null,
    priority = priority,
  )
}

fun List<PrisonApiOffenderAdjudicationHearing>.prisonApiPrisonOffenderAdjudicationsToScheduledEvents(
  prisonCode: String,
  priority: Int,
): List<ModelScheduleEvent> =
  map { it.toScheduledEvent(prisonCode, priority) }

fun PrisonApiOffenderAdjudicationHearing.toScheduledEvent(
  prisonCode: String,
  priority: Int,
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
    priority = priority,
  )
}

fun List<PrisonApiScheduledEvent>.prisonApiAppointmentsToScheduledEvents(prisonerNumber: String?, priority: Int) =
  prisonApiScheduledEventToScheduledEvents(prisonerNumber, EventType.APPOINTMENT, priority)

fun List<PrisonApiScheduledEvent>.prisonApiVisitsToScheduledEvents(prisonerNumber: String?, priority: Int) =
  prisonApiScheduledEventToScheduledEvents(prisonerNumber, EventType.VISIT, priority)

fun List<PrisonApiScheduledEvent>.prisonApiActivitiesToScheduledEvents(prisonerNumber: String?, priority: Int) =
  prisonApiScheduledEventToScheduledEvents(prisonerNumber, EventType.ACTIVITY, priority)

fun List<PrisonApiScheduledEvent>.prisonApiScheduledEventToScheduledEvents(
  prisonerNumber: String?,
  eventType: EventType,
  priority: Int,
) = map {
  ModelScheduleEvent(
    prisonCode = it.agencyId,
    eventId = it.eventId,
    bookingId = it.bookingId,
    locationId = it.eventLocationId,
    location = it.eventLocation,
    eventClass = it.eventClass,
    eventStatus = it.eventStatus,
    eventType = eventType.name,
    eventTypeDesc = it.eventTypeDesc,
    event = it.eventSubType,
    eventDesc = it.eventSubTypeDesc,
    details = it.eventSourceDesc,
    prisonerNumber = prisonerNumber,
    date = it.eventDate,
    startTime = LocalDateTime.parse(it.startTime).toLocalTime(),
    endTime = it.endTime?.let { endTime -> LocalDateTime.parse(endTime).toLocalTime() },
    priority = priority,
  )
}

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

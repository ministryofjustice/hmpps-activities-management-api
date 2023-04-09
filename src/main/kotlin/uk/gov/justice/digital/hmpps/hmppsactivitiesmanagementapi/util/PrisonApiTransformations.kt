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

const val ADJUDICATION_HEARING_DURATION_TWO_HOURS = 2L

fun List<PrisonApiPrisonerSchedule>.multiplePrisonerAppointmentsToScheduledEvents(
  prisonCode: String,
  priority: Int,
) = prisonerScheduleToScheduledEvents(prisonCode, EventType.APPOINTMENT, priority)

fun List<PrisonApiPrisonerSchedule>.multiplePrisonerCourtEventsToScheduledEvents(
  prisonCode: String,
  priority: Int,
) = prisonerScheduleToScheduledEvents(prisonCode, EventType.COURT_HEARING, priority)

fun List<PrisonApiPrisonerSchedule>.multiplePrisonerVisitsToScheduledEvents(
  prisonCode: String,
  priority: Int,
) = prisonerScheduleToScheduledEvents(prisonCode, EventType.VISIT, priority)

fun List<PrisonApiPrisonerSchedule>.multiplePrisonerActivitiesToScheduledEvents(
  prisonCode: String,
  priority: Int,
) = prisonerScheduleToScheduledEvents(prisonCode, EventType.ACTIVITY, priority)

fun List<PrisonApiPrisonerSchedule>.multiplePrisonerTransfersToScheduledEvents(
  prisonCode: String,
  priority: Int,
) = prisonerScheduleToScheduledEvents(prisonCode, EventType.EXTERNAL_TRANSFER, priority)

private fun List<PrisonApiPrisonerSchedule>.prisonerScheduleToScheduledEvents(
  prisonCode: String,
  eventType: EventType,
  priority: Int,
) = map {
  ModelScheduleEvent(
    prisonCode = prisonCode,
    eventSource = "NOMIS",
    eventType = eventType.name,
    eventId = it.eventId,
    bookingId = it.bookingId,
    prisonerNumber = it.offenderNo,
    appointmentInstanceId = null,
    appointmentOccurrenceId = null,
    oicHearingId = null,
    scheduledInstanceId = null,
    internalLocationId = it.locationId,
    internalLocationCode = null,
    internalLocationDescription = it.eventLocation ?: "External",
    cancelled = it.eventStatus == "CANC",
    suspended = false,
    inCell = false,
    summary = it.eventDescription,
    categoryCode = it.event,
    categoryDescription = null,
    comments = it.comment,
    date = LocalDateTime.parse(it.startTime).toLocalDate(),
    startTime = LocalDateTime.parse(it.startTime).toLocalTime(),
    endTime = it.endTime?.let { endTime -> LocalDateTime.parse(endTime).toLocalTime() },
    priority = priority,
  )
}

 /*
 Takes a list of Prison API CourtHearings for a single person from NOMIS.
 Produces a list of SAA ScheduledEvents for court hearings.
 */
fun PrisonApiCourtHearings.nomisCourtHearingsToScheduledEvents(
  bookingId: Long,
  prisonCode: String?,
  prisonerNumber: String?,
  priority: Int,
) = this.hearings?.map {
  ModelScheduleEvent(
    prisonCode = prisonCode,
    eventSource = "NOMIS",
    eventType = EventType.COURT_HEARING.name,
    eventId = it.id,
    bookingId = bookingId,
    appointmentInstanceId = null,
    appointmentOccurrenceId = null,
    oicHearingId = null,
    scheduledInstanceId = null,
    internalLocationId = null,
    internalLocationCode = null,
    internalLocationDescription = it.location?.description,
    cancelled = false,
    inCell = false,
    categoryCode = null,
    categoryDescription = null,
    summary = "Court hearing",
    comments = null,
    prisonerNumber = prisonerNumber,
    date = LocalDateTime.parse(it.dateTime).toLocalDate(),
    startTime = LocalDateTime.parse(it.dateTime).toLocalTime(),
    endTime = null,
    priority = priority,
  )
}

/*
 Takes a list of NOMIS OffenderAdjudicationHearing events (single or multiple people).
 Produces a list of SAA ScheduledEvents for adjudication hearings
 */
fun List<PrisonApiOffenderAdjudicationHearing>.nomisAdjudicationsToScheduledEvents(
  prisonCode: String,
  priority: Int,
): List<ModelScheduleEvent> = map {
  ModelScheduleEvent(
    prisonCode = prisonCode,
    eventSource = "NOMIS",
    oicHearingId = it.hearingId,
    eventType = EventType.ADJUDICATION_HEARING.name,
    bookingId = null,
    prisonerNumber = it.offenderNo,
    internalLocationId = it.internalLocationId,
    internalLocationCode = null,
    internalLocationDescription = it.internalLocationDescription,
    appointmentInstanceId = null,
    appointmentOccurrenceId = null,
    scheduledInstanceId = null,
    eventId = null,
    summary = it.hearingType,
    comments = null,
    categoryCode = null,
    categoryDescription = it.hearingType,
    cancelled = it.eventStatus == "CANC",
    suspended = false,
    date = LocalDateTime.parse(it.startTime).toLocalDate(),
    startTime = LocalDateTime.parse(it.startTime).toLocalTime(),
    endTime = LocalDateTime.parse(it.startTime).toLocalTime().plusHours(ADJUDICATION_HEARING_DURATION_TWO_HOURS),
    priority = priority,
  )
}

/*
 Takes a list of NOMIS appointment events for a single person.
 Produces a list of SAA ScheduledEvents for appointments.
 */
fun List<PrisonApiScheduledEvent>.nomisAppointmentsToScheduledEvents(
  prisonerNumber: String?,
  priority: Int,
) = map {
  ModelScheduleEvent(
    prisonCode = it.agencyId,
    eventSource = "NOMIS",
    eventType = EventType.APPOINTMENT.name,
    eventId = it.eventId,
    appointmentInstanceId = null,
    appointmentOccurrenceId = null,
    oicHearingId = null,
    scheduledInstanceId = null,
    bookingId = it.bookingId,
    internalLocationId = it.eventLocationId,
    internalLocationCode = it.eventLocation,
    internalLocationDescription = it.eventLocation,
    cancelled = it.eventStatus == "CANC",
    inCell = false,
    categoryCode = it.eventSubType,
    categoryDescription = it.eventSubTypeDesc,
    summary = "Appointment ${it.eventSubTypeDesc}",
    comments = it.outcomeComment,
    prisonerNumber = prisonerNumber,
    date = it.eventDate,
    startTime = LocalDateTime.parse(it.startTime).toLocalTime(),
    endTime = it.endTime?.let { endTime -> LocalDateTime.parse(endTime).toLocalTime() },
    priority = priority,
  )
}

/*
 Takes a list of NOMIS visit events for one person.
 Produces a list of SAA scheduled events for visits.
 */
fun List<PrisonApiScheduledEvent>.nomisVisitsToScheduledEvents(
  prisonerNumber: String?,
  priority: Int,
) = map {
  ModelScheduleEvent(
    prisonCode = it.agencyId,
    eventSource = "NOMIS",
    eventType = EventType.VISIT.name,
    eventId = it.eventId,
    appointmentInstanceId = null,
    appointmentOccurrenceId = null,
    oicHearingId = null,
    scheduledInstanceId = null,
    bookingId = it.bookingId,
    internalLocationId = it.eventLocationId,
    internalLocationCode = it.eventLocation,
    internalLocationDescription = it.eventLocation,
    cancelled = it.eventStatus == "CANC",
    inCell = false,
    categoryCode = it.eventSubType,
    categoryDescription = it.eventSubTypeDesc,
    summary = "Visit ${it.eventSubTypeDesc}",
    comments = it.outcomeComment,
    prisonerNumber = prisonerNumber,
    date = it.eventDate,
    startTime = LocalDateTime.parse(it.startTime).toLocalTime(),
    endTime = it.endTime?.let { endTime -> LocalDateTime.parse(endTime).toLocalTime() },
    priority = priority,
  )
}

/*
 Takes a list of NOMIS activity events for a single person.
 Produces a list of SAA scheduled events for activities.
 */
fun List<PrisonApiScheduledEvent>.nomisActivitiesToScheduledEvents(
  prisonerNumber: String?,
  priority: Int,
) = map {
  ModelScheduleEvent(
    prisonCode = it.agencyId,
    eventSource = "NOMIS",
    eventType = EventType.ACTIVITY.name,
    eventId = it.eventId,
    appointmentInstanceId = null,
    appointmentOccurrenceId = null,
    oicHearingId = null,
    scheduledInstanceId = null,
    bookingId = it.bookingId,
    internalLocationId = it.eventLocationId,
    internalLocationCode = it.eventLocation,
    internalLocationDescription = it.eventLocation,
    cancelled = it.eventStatus == "CANC",
    inCell = false,
    categoryCode = it.eventSubType,
    categoryDescription = it.eventSubTypeDesc,
    summary = it.eventSubTypeDesc,
    comments = it.outcomeComment,
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

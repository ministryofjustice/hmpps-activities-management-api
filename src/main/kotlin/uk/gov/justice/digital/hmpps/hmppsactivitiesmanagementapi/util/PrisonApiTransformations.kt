package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonLocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.internalLocationId
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings as PrisonApiCourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location as PrisonApiLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing as PrisonApiOffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule as PrisonApiPrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode as PrisonApiReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail as PrisonApiUserDetail
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
  prisonLocations: PrisonLocations = emptyMap(),
) = prisonerScheduleToScheduledEvents(prisonCode, EventType.VISIT, priority, prisonLocations)

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
  prisonLocations: PrisonLocations = emptyMap(),
) = map {
  val mayBeInternalLocation = it.locationId?.let(prisonLocations::get)

  ModelScheduleEvent(
    prisonCode = prisonCode,
    eventSource = "NOMIS",
    eventType = eventType.name,
    eventId = it.eventId,
    bookingId = it.bookingId,
    prisonerNumber = it.offenderNo,
    appointmentSeriesId = null,
    appointmentId = null,
    appointmentAttendeeId = null,
    oicHearingId = null,
    scheduledInstanceId = null,
    internalLocationId = it.locationId,
    internalLocationCode = null,
    internalLocationUserDescription = mayBeInternalLocation?.userDescription,
    internalLocationDescription = mayBeInternalLocation?.description ?: (it.eventLocation ?: "External"),
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
fun PrisonApiCourtHearings?.nomisCourtHearingsToScheduledEvents(
  bookingId: Long,
  prisonCode: String?,
  prisonerNumber: String?,
  priority: Int,
) = this?.hearings?.map {
  ModelScheduleEvent(
    prisonCode = prisonCode,
    eventSource = "NOMIS",
    eventType = EventType.COURT_HEARING.name,
    eventId = it.id,
    bookingId = bookingId,
    appointmentSeriesId = null,
    appointmentId = null,
    appointmentAttendeeId = null,
    oicHearingId = null,
    scheduledInstanceId = null,
    internalLocationId = null,
    internalLocationCode = null,
    internalLocationUserDescription = null,
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
} ?: emptyList()

/*
 Takes a list of NOMIS OffenderAdjudicationHearing events (single or multiple people).
 Produces a list of SAA ScheduledEvents for adjudication hearings
 */
fun List<PrisonApiOffenderAdjudicationHearing>.nomisAdjudicationsToScheduledEvents(
  prisonCode: String,
  priority: Int,
  prisonLocations: PrisonLocations,
): List<ModelScheduleEvent> = map {
  val mayBeInternalLocation = it.internalLocationId.let(prisonLocations::get)

  ModelScheduleEvent(
    prisonCode = prisonCode,
    eventSource = "NOMIS",
    oicHearingId = it.hearingId,
    eventType = EventType.ADJUDICATION_HEARING.name,
    bookingId = null,
    prisonerNumber = it.offenderNo,
    internalLocationId = it.internalLocationId,
    internalLocationCode = null,
    internalLocationUserDescription = mayBeInternalLocation?.userDescription,
    internalLocationDescription = mayBeInternalLocation?.description ?: it.internalLocationDescription,
    appointmentSeriesId = null,
    appointmentId = null,
    appointmentAttendeeId = null,
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
    endTime = it.startTime?.let { startTime ->
      LocalDateTime.parse(startTime).toLocalTime().plusHours(ADJUDICATION_HEARING_DURATION_TWO_HOURS)
    },
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
  prisonLocations: PrisonLocations,
) = map {
  val mayBeInternalLocation = it.internalLocationId().let(prisonLocations::get)

  ModelScheduleEvent(
    prisonCode = it.agencyId,
    eventSource = "NOMIS",
    eventType = EventType.APPOINTMENT.name,
    eventId = it.eventId,
    appointmentSeriesId = null,
    appointmentId = null,
    appointmentAttendeeId = null,
    oicHearingId = null,
    scheduledInstanceId = null,
    bookingId = it.bookingId,
    internalLocationId = it.eventLocationId,
    internalLocationCode = it.eventLocation,
    internalLocationUserDescription = mayBeInternalLocation?.userDescription,
    internalLocationDescription = mayBeInternalLocation?.description ?: it.eventLocation,
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
  prisonLocations: PrisonLocations,
) = map {
  val mayBeInternalLocation = it.internalLocationId().let(prisonLocations::get)

  ModelScheduleEvent(
    prisonCode = it.agencyId,
    eventSource = "NOMIS",
    eventType = EventType.VISIT.name,
    eventId = it.eventId,
    appointmentSeriesId = null,
    appointmentId = null,
    appointmentAttendeeId = null,
    oicHearingId = null,
    scheduledInstanceId = null,
    bookingId = it.bookingId,
    internalLocationId = it.internalLocationId(),
    internalLocationUserDescription = mayBeInternalLocation?.userDescription,
    internalLocationCode = it.eventLocation,
    internalLocationDescription = mayBeInternalLocation?.description ?: it.eventLocation,
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
  prisonLocations: PrisonLocations,
) = map {
  val mayBeInternalLocation = it.internalLocationId().let(prisonLocations::get)

  ModelScheduleEvent(
    prisonCode = it.agencyId,
    eventSource = "NOMIS",
    eventType = EventType.ACTIVITY.name,
    eventId = it.eventId,
    appointmentSeriesId = null,
    appointmentId = null,
    appointmentAttendeeId = null,
    oicHearingId = null,
    scheduledInstanceId = null,
    bookingId = it.bookingId,
    internalLocationId = it.eventLocationId,
    internalLocationCode = it.eventLocation,
    internalLocationUserDescription = mayBeInternalLocation?.userDescription,
    internalLocationDescription = mayBeInternalLocation?.description ?: it.eventLocation,
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
    ModelAppointmentCategorySummary(code, code)
  } else {
    ModelAppointmentCategorySummary(this.code, this.description)
  }

fun PrisonApiReferenceCode?.toAppointmentName(code: String, description: String?) =
  this.toAppointmentCategorySummary(code).description.let { category ->
    if (!description.isNullOrEmpty()) "$description ($category)" else category
  }

fun List<PrisonApiReferenceCode>.toAppointmentCategorySummary() = map { it.toAppointmentCategorySummary(it.code) }

fun PrisonApiLocation?.toAppointmentLocationSummary(locationId: Long, prisonCode: String) =
  if (this == null) {
    ModelAppointmentLocationSummary(locationId, prisonCode, "No information available")
  } else {
    ModelAppointmentLocationSummary(this.locationId, this.agencyId, this.userDescription ?: this.description)
  }

fun List<PrisonApiLocation>.toAppointmentLocation() =
  map { it.toAppointmentLocationSummary(it.locationId, it.agencyId) }

fun PrisonApiUserDetail?.toSummary(username: String) =
  if (this == null) {
    ModelUserSummary(-1, username, "UNKNOWN", "UNKNOWN")
  } else {
    ModelUserSummary(this.staffId, this.username, this.firstName, this.lastName)
  }

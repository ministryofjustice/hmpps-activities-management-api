package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonLocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing as PrisonApiOffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule as PrisonApiPrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent as ModelScheduleEvent

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
  date: LocalDate? = null,
) = prisonerScheduleToScheduledEvents(prisonCode, EventType.EXTERNAL_TRANSFER, priority, date = date)

private fun List<PrisonApiPrisonerSchedule>.prisonerScheduleToScheduledEvents(
  prisonCode: String,
  eventType: EventType,
  priority: Int,
  prisonLocations: PrisonLocations = emptyMap(),
  date: LocalDate? = null,
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
    date = it.startTime?.let { startTime -> LocalDateTime.parse(startTime).toLocalDate() } ?: date,
    startTime = it.startTime?.let { startTime -> LocalDateTime.parse(startTime).toLocalTime() },
    endTime = it.endTime?.let { endTime -> LocalDateTime.parse(endTime).toLocalTime() },
    priority = priority,
    appointmentSeriesCancellationStartDate = null,
    appointmentSeriesCancellationStartTime = null,
    appointmentSeriesFrequency = null,
    paidActivity = null,
    issuePayment = null,
    attendanceStatus = null,
    attendanceReasonCode = null,
  )
}

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
    appointmentSeriesCancellationStartDate = null,
    appointmentSeriesCancellationStartTime = null,
    appointmentSeriesFrequency = null,
    paidActivity = null,
    issuePayment = null,
    attendanceStatus = null,
    attendanceReasonCode = null,
  )
}

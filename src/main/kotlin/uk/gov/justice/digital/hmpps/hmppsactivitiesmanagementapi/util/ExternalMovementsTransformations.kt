package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.EventPriorities

fun List<ExternalMovement>.toScheduledEvents(prisonCode: String, priorities: EventPriorities): List<ScheduledEvent> = map { it.toScheduledEvent(prisonCode, priorities) }

fun ExternalMovement.toScheduledEvent(prisonCode: String, priorities: EventPriorities) = ScheduledEvent(
  prisonCode = prisonCode,
  eventSource = "EXTERNAL_MOVEMENTS_API",
  eventType = EventType.ACTIVITY.name,
  scheduledInstanceId = null,
  appointmentSeriesId = null,
  appointmentId = null,
  appointmentAttendeeId = null,
  oicHearingId = null,
  eventId = null,
  bookingId = null,
  internalLocationId = null,
  internalLocationCode = null,
  internalLocationUserDescription = null,
  internalLocationDescription = null,
  categoryCode = description.code,
  categoryDescription = null,
  summary = transformedSummary(),
  comments = null,
  outsidePrison = true,
  prisonerNumber = prisonerNumber,
  date = start.toLocalDate(),
  startTime = start.toLocalTime(),
  endTime = end.toLocalTime(),
  priority = priorities.getOrDefault(EventType.ACTIVITY),
  appointmentSeriesCancellationStartDate = null,
  appointmentSeriesCancellationStartTime = null,
  appointmentSeriesFrequency = null,
  paidActivity = null,
  issuePayment = null,
  attendanceStatus = null,
  attendanceReasonCode = null,
  status = status.description,
)

private fun ExternalMovement.transformedSummary(): String = when {
  isSensitive -> "ROTL or other temporary absence"
  description.code == "FB" -> "Accommodation-related ROTL"
  description.code in listOf("YOTR", "20") -> "Sentence or resettlement plan ROTL"
  else -> description.short
}

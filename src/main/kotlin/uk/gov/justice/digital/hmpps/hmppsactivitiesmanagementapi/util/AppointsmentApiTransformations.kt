package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location as PrisonApiLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule as PrisonApiPrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ReferenceCode as PrisonApiReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner as PrisonerSearchApiPrisoner

/**
 * Transform functions providing a thin layer to transform appointment entities into their API model equivalents and vice-versa.
 */

/**
 * Maps List<AppointmentInstance> to List<ScheduledEvent>
 */
fun List<AppointmentInstance>.toScheduledEvent(
  referenceCodeMap: Map<String, PrisonApiReferenceCode>,
  eventType: String,
  eventTypeDesc: String,
  eventClass: String,
  eventStatus: String,
  eventSource: String,
) = map {
  val category = referenceCodeMap[it.categoryCode].toAppointmentCategorySummary(it.categoryCode)
  PrisonApiScheduledEvent(
    bookingId = it.bookingId,
    startTime = LocalDateTime.of(it.appointmentDate, it.startTime).toIsoDateTime(),
    endTime = it.endTime?.let { _ ->
      LocalDateTime.of(it.appointmentDate, it.endTime).toIsoDateTime()
    },
    eventType = eventType,
    eventTypeDesc = eventTypeDesc,
    eventClass = eventClass,
    eventId = it.appointmentOccurrenceId,
    eventStatus = eventStatus,
    eventDate = it.appointmentDate,
    eventSource = eventSource,
    eventSubType = category.code,
    eventSubTypeDesc = category.description,
    agencyId = it.prisonCode,
  )
}

/**
 * Maps a List<AppointmentInstance> to a List<PrisonerSchedule>
 *
 *   @param prisonerLookup Map of prisonerNumber -> Prisoner to facilitate data lookup
 *   @param locationLookup Map of locationId -> Location to facilitate data lookup
 */
fun List<AppointmentInstance>.toPrisonerSchedule(
  referenceCodeMap: Map<String, PrisonApiReferenceCode>,
  prisonerLookup: Map<String, PrisonerSearchApiPrisoner>,
  locationLookup: Map<Long, PrisonApiLocation>,
  eventType: String,
  eventStatus: String,
) = map {
  val category = referenceCodeMap[it.categoryCode].toAppointmentCategorySummary(it.categoryCode)
  PrisonApiPrisonerSchedule(
    cellLocation = prisonerLookup[it.prisonerNumber]?.cellLocation!!,
    comment = it.comment,
    event = category.code,
    eventDescription = category.description,
    eventLocation = locationLookup[it.internalLocationId]?.userDescription,
    eventStatus = eventStatus,
    eventType = eventType,
    firstName = prisonerLookup[it.prisonerNumber]?.firstName!!,
    lastName = prisonerLookup[it.prisonerNumber]?.lastName!!,
    locationId = it.internalLocationId,
    offenderNo = it.prisonerNumber,
    startTime = LocalDateTime.of(it.appointmentDate, it.startTime).toIsoDateTime(),
    endTime = it.endTime?.let { _ ->
      LocalDateTime.of(it.appointmentDate, it.endTime).toIsoDateTime()
    },
  )
}

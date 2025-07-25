package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import java.time.LocalDate

object PrisonApiPrisonerScheduleFixture {

  // From prisonAPI
  fun courtInstance(
    offenderNo: String = "G4793VF",
    bookingId: Long? = 900001,
    locationId: Long? = 1,
    eventId: Long = 1,
    firstName: String = "TIM",
    lastName: String = "HARRISON",
    cellLocation: String? = "2-1-001",
    event: String = "CRT",
    eventType: String = "COURT",
    eventDescription: String = "Court Appearance",
    eventLocation: String? = "Leeds Crown Court",
    eventStatus: String = "EXP",
    comment: String? = null,
    date: LocalDate,
    startTime: String = date.atStartOfDay().toIsoDateTime(),
  ) = PrisonerSchedule(
    offenderNo = offenderNo,
    bookingId = bookingId,
    locationId = locationId,
    eventId = eventId,
    firstName = firstName,
    lastName = lastName,
    cellLocation = cellLocation,
    event = event,
    eventType = eventType,
    eventDescription = eventDescription,
    eventLocation = eventLocation,
    eventStatus = eventStatus,
    comment = comment,
    startTime = startTime,
  )

  // From prisonAPI
  fun appointmentInstance(
    offenderNo: String = "G4793VF",
    bookingId: Long? = 900001,
    locationId: Long? = 1,
    eventId: Long? = 1,
    firstName: String = "TIM",
    lastName: String = "HARRISON",
    cellLocation: String? = "2-1-001",
    event: String = "GOVE",
    eventType: String? = "APPT",
    eventDescription: String = "Governor",
    eventLocation: String? = "INTERVIEW ROOM",
    eventStatus: String? = "SCH",
    comment: String? = "Dont be late",
    date: LocalDate,
    startTime: String = date.atStartOfDay().toIsoDateTime(),
    endTime: String = date.atStartOfDay().plusHours(12).toIsoDateTime(),
  ) = PrisonerSchedule(
    offenderNo = offenderNo,
    bookingId = bookingId,
    locationId = locationId,
    eventId = eventId,
    firstName = firstName,
    lastName = lastName,
    cellLocation = cellLocation,
    event = event,
    eventType = eventType,
    eventDescription = eventDescription,
    eventLocation = eventLocation,
    eventStatus = eventStatus,
    comment = comment,
    startTime = startTime,
    endTime = endTime,
  )

  // From prisonAPI
  fun visitInstance(
    offenderNo: String = "G4793VF",
    bookingId: Long? = 900001,
    locationId: Long? = 1,
    eventId: Long? = 1,
    firstName: String = "TIM",
    lastName: String = "HARRISON",
    cellLocation: String? = "2-1-001",
    event: String = "VISIT",
    eventType: String? = "VISIT",
    eventDescription: String = "Visit",
    eventLocation: String? = "INTERVIEW ROOM",
    eventStatus: String? = "EXP",
    comment: String? = null,
    date: LocalDate,
    startTime: String = date.atStartOfDay().toIsoDateTime(),
  ) = PrisonerSchedule(
    offenderNo = offenderNo,
    bookingId = bookingId,
    locationId = locationId,
    eventId = eventId,
    firstName = firstName,
    lastName = lastName,
    cellLocation = cellLocation,
    event = event,
    eventType = eventType,
    eventDescription = eventDescription,
    eventLocation = eventLocation,
    eventStatus = eventStatus,
    comment = comment,
    startTime = startTime,
  )

  // From prisonAPI
  fun activityInstance(
    offenderNo: String = "G4793VF",
    locationId: Long? = 26958,
    eventId: Long? = 1,
    firstName: String = "TIM",
    lastName: String = "HARRISON",
    cellLocation: String? = "1-2-003",
    event: String = "ACTIVITY",
    eventType: String? = "PA",
    eventDescription: String = "Activity",
    eventLocation: String? = "Workshop 1",
    eventStatus: String? = "EXP",
    comment: String? = null,
    date: LocalDate,
    startTime: String = date.atStartOfDay().toIsoDateTime(),
  ) = PrisonerSchedule(
    offenderNo = offenderNo,
    locationId = locationId,
    eventId = eventId,
    firstName = firstName,
    lastName = lastName,
    cellLocation = cellLocation,
    event = event,
    eventType = eventType,
    eventDescription = eventDescription,
    eventLocation = eventLocation,
    eventStatus = eventStatus,
    comment = comment,
    startTime = startTime,
  )

  fun transferInstance(
    offenderNo: String = "G4793VF",
    bookingId: Long? = 900001,
    eventId: Long? = 1,
    firstName: String = "TIM",
    lastName: String = "HARRISON",
    cellLocation: String? = "2-1-001",
    event: String = "TRANSFER",
    eventType: String? = "TRANSFER",
    eventDescription: String = "Transfer",
    eventStatus: String? = "SCH",
    date: LocalDate,
    startTime: String? = date.atStartOfDay().toIsoDateTime(),
    endTime: String? = date.atStartOfDay().plusHours(12).toIsoDateTime(),
  ) = PrisonerSchedule(
    offenderNo = offenderNo,
    bookingId = bookingId,
    locationId = null,
    eventId = eventId,
    firstName = firstName,
    lastName = lastName,
    cellLocation = cellLocation,
    event = event,
    eventType = eventType,
    eventDescription = eventDescription,
    eventLocation = "Should not be included",
    eventStatus = eventStatus,
    comment = "Should not be included",
    startTime = startTime,
    endTime = endTime,
  )
}

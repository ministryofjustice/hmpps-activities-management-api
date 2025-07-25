package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun internalLocation(
  locationId: Long = 1L,
  locationType: String = "AREA",
  description: String = "EDUC-ED1-ED1",
  prisonCode: String = "MDI",
  locationUsage: String = "PROG",
  userDescription: String? = "Education 1",
) = Location(
  locationId = locationId,
  locationType = locationType,
  description = description,
  agencyId = prisonCode,
  locationUsage = locationUsage,
  locationPrefix = "$prisonCode-$description",
  userDescription = userDescription,
)

fun appointmentLocation(
  locationId: Long,
  prisonCode: String,
  description: String = "Test Appointment Location",
  userDescription: String = "Test Appointment Location User Description",
) = Location(
  locationId = locationId,
  locationType = "APP",
  description = description,
  locationUsage = "APP",
  agencyId = prisonCode,
  currentOccupancy = 2,
  userDescription = userDescription,
)

fun appointmentCategoryReferenceCode(code: String = "TEST", description: String = "Test Category") = ReferenceCode(
  domain = "INT_SCH_RSN",
  code = code,
  description = description,
  activeFlag = "Y",
)

fun prisonerTransfer(
  offenderNo: String = "G4793VF",
  bookingId: Long? = 1,
  eventId: Long? = 1,
  firstName: String = "FRED",
  lastName: String = "BLOGGS",
  cellLocation: String = "2-1-001",
  event: String = "TRANSFER",
  eventType: String = "TRANSFER",
  eventDescription: String = "Governor",
  eventStatus: String = "SCH",
  date: LocalDate?,
  startTime: String? = date?.atStartOfDay()?.toIsoDateTime(),
  endTime: String? = date?.atStartOfDay()?.plusHours(12)?.toIsoDateTime(),
) = PrisonerSchedule(
  offenderNo = offenderNo,
  bookingId = bookingId,
  locationId = -1,
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

fun adjudicationHearing(
  prisonCode: String = MOORLAND_PRISON_CODE,
  offenderNo: String = "1234567890",
  hearingId: Long = -1,
  hearingType: String = "Governor's Hearing Adult",
  startTime: LocalDateTime = LocalDate.now().atStartOfDay(),
  internalLocationId: Long = -2,
  internalLocationDescription: String? = "Adjudication room",
  eventStatus: String = "SCH",
) = OffenderAdjudicationHearing(
  agencyId = prisonCode,
  offenderNo = offenderNo,
  hearingId = hearingId,
  hearingType = hearingType,
  startTime = startTime.toIsoDateTime(),
  internalLocationId = internalLocationId,
  internalLocationDescription = internalLocationDescription,
  eventStatus = eventStatus,
)

fun movement(
  prisonerNumber: String = "A1179MT",
  fromPrisonCode: String = MOORLAND_PRISON_CODE,
  movementDate: LocalDate = LocalDate.now(),
  movementTime: LocalTime = LocalTime.now(),
  movementType: Movement.MovementType = Movement.MovementType.TRN,
) = Movement(
  offenderNo = prisonerNumber,
  createDateTime = TimeSource.now().toIsoDateTime(),
  fromAgency = fromPrisonCode,
  fromAgencyDescription = "Moorland",
  toAgency = "OUT",
  toAgencyDescription = "Outside",
  fromCity = "",
  toCity = "",
  movementType = movementType,
  movementTypeDescription = movementType.value,
  directionCode = "OUT",
  movementDate = movementDate,
  movementTime = movementTime.toIsoTime(),
  movementReason = "Abscond",
)

fun visit(prisonerNumber: String = "G4793VF", locationId: Long = -1, dateTime: LocalDateTime) = PrisonerSchedule(
  offenderNo = prisonerNumber,
  locationId = locationId,
  firstName = "Fred",
  lastName = "Bloggs",
  cellLocation = null,
  event = "event code",
  eventType = "VISIT",
  eventDescription = "visit event description",
  eventLocationId = locationId,
  eventLocation = "visit event location",
  eventStatus = null,
  startTime = dateTime.toIsoDateTime(),
  comment = "visit comments",
)

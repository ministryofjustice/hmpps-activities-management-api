package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CaseLoad
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import java.time.LocalDate
import java.time.LocalDateTime

fun userDetail(
  id: Long = 1,
  username: String = "TEST.USER",
  firstName: String = "TEST",
  lastName: String = "USER",
) = UserDetail(
  staffId = id,
  username = username,
  firstName = firstName,
  lastName = lastName,
  accountStatus = UserDetail.AccountStatus.ACTIVE,
  active = true,
  lockDate = "",
)

fun userCaseLoads(prisonCode: String) =
  listOf(
    CaseLoad(
      caseLoadId = prisonCode,
      description = "Prison Description",
      type = CaseLoad.Type.INST,
      currentlyActive = true,
    ),
  )

fun appointmentLocation(locationId: Long, prisonCode: String, description: String = "Test Appointment Location", userDescription: String = "Test Appointment Location User Description") =
  Location(
    locationId = locationId,
    locationType = "APP",
    description = description,
    locationUsage = "APP",
    agencyId = prisonCode,
    currentOccupancy = 2,
    userDescription = userDescription,
  )

fun appointmentCategoryReferenceCode(code: String = "TEST", description: String = "Test Category") =
  ReferenceCode(
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
  date: LocalDate,
  startTime: String = date.atStartOfDay().toIsoDateTime(),
  endTime: String = date.atStartOfDay().plusHours(12).toIsoDateTime(),
) =
  PrisonerSchedule(
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
  prisonCode: String = moorlandPrisonCode,
  offenderNo: String = "1234567890",
  hearingId: Long = -1,
  hearingType: String = "Governor's Hearing Adult",
  startTime: LocalDateTime = LocalDate.now().atStartOfDay(),
  internalLocationId: Long = -2,
  internalLocationDescription: String = "Adjudication room",
  eventStatus: String = "SCH",
) =
  OffenderAdjudicationHearing(
    agencyId = prisonCode,
    offenderNo = offenderNo,
    hearingId = hearingId,
    hearingType = hearingType,
    startTime = startTime.toIsoDateTime(),
    internalLocationId = internalLocationId,
    internalLocationDescription = internalLocationDescription,
    eventStatus = eventStatus,
  )

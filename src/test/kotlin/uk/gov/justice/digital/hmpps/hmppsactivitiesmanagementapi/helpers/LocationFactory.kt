package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService.LocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerScheduledEventsFixture.activityInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerScheduledEventsFixture.appointmentInstance
import java.util.UUID

fun location(
  agencyId: String = MOORLAND_PRISON_CODE,
  userDescription: String = "User Description",
  description: String = "Some Description",
  locationId: Long = 123L,
  locationType: String = "Some Type",
) = Location(
  agencyId = agencyId,
  description = description,
  locationId = locationId,
  locationType = locationType,
  userDescription = userDescription,
)

fun locations(
  agencyId: String = "PBI",
  description: String = "Some Description",
  userDescription: String = "User Description",
  locationId: Long = 123L,
  locationType: String = "Some Type",
) = mapOf(
  locationId to Location(
    agencyId = agencyId,
    description = description,
    locationId = locationId,
    locationType = locationType,
    userDescription = userDescription,
  ),
)

fun locationDetails(
  locationId: Long,
  dpsLocationId: UUID,
  agencyId: String,
  description: String = "Test Location",
  code: String = "WW",
) = LocationDetails(
  locationId = locationId,
  dpsLocationId = dpsLocationId,
  code = code,
  description = description,
  agencyId = agencyId,
)

fun appointmentLocationDetails(
  locationId: Long,
  dpsLocationId: UUID,
  agencyId: String,
  description: String = "Test Appointment Location",
) = locationDetails(
  locationId = locationId,
  dpsLocationId = dpsLocationId,
  code = "WW",
  description = description,
  agencyId = agencyId,
)

fun internalLocationEventsSummary(
  id: Long = 1L,
  dpsLocationId: UUID = UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc"),
  prisonCode: String = "MDI",
  code: String = "EDUC-ED1-ED1",
  description: String = "Education 1",
) = InternalLocationEventsSummary(
  id,
  dpsLocationId,
  prisonCode,
  code,
  description,
)

fun internalLocationEvents(
  id: Long = 1L,
  dpsLocationId: UUID = UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc"),
  prisonCode: String = "MDI",
  code: String = "EDUC-ED1-ED1",
  description: String = "Education 1",
) = InternalLocationEvents(
  id,
  dpsLocationId,
  prisonCode,
  code,
  description,
  setOf(activityInstance(), appointmentInstance()),
)

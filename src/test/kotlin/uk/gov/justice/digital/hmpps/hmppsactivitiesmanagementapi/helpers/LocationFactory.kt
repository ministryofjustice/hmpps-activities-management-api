package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerScheduledEventsFixture.activityInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerScheduledEventsFixture.appointmentInstance

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

fun internalLocationEventsSummary(
  id: Long = 1L,
  prisonCode: String = "MDI",
  code: String = "EDUC-ED1-ED1",
  description: String = "Education 1",
) = InternalLocationEventsSummary(
  id,
  prisonCode,
  code,
  description,
)

fun internalLocationEvents(
  id: Long = 1L,
  prisonCode: String = "MDI",
  code: String = "EDUC-ED1-ED1",
  description: String = "Education 1",
) = InternalLocationEvents(
  id,
  prisonCode,
  code,
  description,
  setOf(activityInstance(), appointmentInstance()),
)

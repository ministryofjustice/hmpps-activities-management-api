package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location

fun locations(
  agencyId: String = "PBI",
  description: String = "Some Description",
  userDescription: String = "User Description",
  locationId: Long = 123L,
  locationType: String = "Some Type",
) = listOf(
  Location(
    agencyId = agencyId,
    description = description,
    locationId = locationId,
    locationType = locationType,
    userDescription = userDescription,
  ),
)

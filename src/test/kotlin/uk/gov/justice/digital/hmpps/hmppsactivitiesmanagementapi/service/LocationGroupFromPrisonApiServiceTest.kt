package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location

class LocationGroupFromPrisonApiServiceTest {

  private val prisonApiClient: PrisonApiClient = mock()
  private val service = LocationGroupFromPrisonApiService(prisonApiClient)

  private val cellA1: Location =
    aLocation(locationId = -320L, locationType = "CELL", description = "LEI-A-1-001", parentLocationId = -32L)
  private val cellAa1: Location =
    aLocation(locationId = -320L, locationType = "CELL", description = "LEI-AA-1-001", parentLocationId = -32L)
  private val cellA3: Location =
    aLocation(locationId = -320L, locationType = "CELL", description = "LEI-A-3-001", parentLocationId = -32L)
  private val cellB1: Location =
    aLocation(locationId = -320L, locationType = "CELL", description = "LEI-B-2-001", parentLocationId = -32L)

  @Test
  fun locationGroupFilters() {
    val filter = service.locationGroupFilter("LEI", "A")
    assertThat(listOf(cellA1, cellA3, cellB1, cellAa1).filter(filter::test))
      .containsExactlyInAnyOrder(cellA1, cellA3)
  }

  private fun aLocation(locationId: Long, locationType: String, description: String, parentLocationId: Long): Location = Location(
    locationId = locationId,
    locationType = locationType,
    description = description,
    locationUsage = "",
    agencyId = "",
    parentLocationId = parentLocationId,
    currentOccupancy = 0,
    locationPrefix = description,
    operationalCapacity = 0,
    userDescription = "",
    internalLocationCode = "",
  )
}

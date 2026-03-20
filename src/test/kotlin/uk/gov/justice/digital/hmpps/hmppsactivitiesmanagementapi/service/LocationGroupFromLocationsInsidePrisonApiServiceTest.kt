package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api.LocationsInsidePrisonAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup

class LocationGroupFromLocationsInsidePrisonApiServiceTest {

  private val locationsInsidePrisonApiClient: LocationsInsidePrisonAPIClient = mock()
  private val service = LocationGroupFromLocationsInsidePrisonApiService(locationsInsidePrisonApiClient)

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

  @Test
  fun `getLocationGroups returns groups from the API client`() {
    runBlocking {
      val locationGroup = LocationGroup(name = "Wing A", key = "A", children = emptyList())

      whenever(locationsInsidePrisonApiClient.getLocationGroups("LEI")).thenReturn(listOf(locationGroup))

      val result = service.getLocationGroups("LEI")

      assertThat(result).isNotNull
      assertThat(result).hasSize(1)

      result.first().apply {
        assertThat(name).isEqualTo("Wing A")
        assertThat(key).isEqualTo("A")
        assertThat(children).isEmpty()
      }
      verifyBlocking(locationsInsidePrisonApiClient) { getLocationGroups("LEI") }
    }
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

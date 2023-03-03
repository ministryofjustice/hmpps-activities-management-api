package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationIdAndDescription
import java.util.Properties
import java.util.function.Predicate

class LocationServiceTest {

  private val prisonApiClient: PrisonApiClient = mock()
  private val locationGroupService: LocationGroupService = mock()
  private val groupsProperties: Properties = mock()

  private val locationService = LocationService(prisonApiClient, locationGroupService, groupsProperties)

  private val cell1 = aLocation(locationPrefix = "cell1")
  private val cell2 = aLocation(locationPrefix = "cell2")
  private val cell3 = aLocation(locationPrefix = "cell3")
  private val cell4 = aLocation(locationPrefix = "cell4")

  @Test
  fun `getVideoLinkRoomsForPrison - should return id and user-friendly location description but only for VIDE location types`() {
    val location1 = Location(
      locationId = 1, locationType = "VIDE", description = "video-room-a", locationUsage = "APP",
      agencyId = "MDI", parentLocationId = 123, currentOccupancy = 2, locationPrefix = "MDI-prefix",
      operationalCapacity = 2, userDescription = "Video Room A", internalLocationCode = "Room 1",
    )

    val location2 = Location(
      locationId = 2, locationType = "VIDE", description = "video-room-b", locationUsage = "APP",
      agencyId = "MDI", parentLocationId = 123, currentOccupancy = 2, locationPrefix = "MDI-prefix",
      operationalCapacity = 2, userDescription = "Video Room B", internalLocationCode = "Room 2",
    )

    val location3 = Location(
      locationId = 3, locationType = "MEETING ROOM", description = "video-room-c", locationUsage = "APP",
      agencyId = "MDI", parentLocationId = 123, currentOccupancy = 2, locationPrefix = "MDI-prefix",
      operationalCapacity = 2, userDescription = "Video Room C", internalLocationCode = "Room 3",
    )

    whenever(prisonApiClient.getLocationsForTypeUnrestricted("MDI", "APP"))
      .thenReturn(Mono.just(listOf(location1, location2, location3)))

    assertThat(locationService.getVideoLinkRoomsForPrison("MDI"))
      .isEqualTo(
        listOf(
          LocationIdAndDescription(locationId = 1, description = "Video Room A"),
          LocationIdAndDescription(locationId = 2, description = "Video Room B"),
        ),
      )
      .doesNotContain(LocationIdAndDescription(locationId = 3, description = "Video Room C"))
  }

  @Test
  fun `getVideoLinkRoomsForPrison - should return id plus hyphenated description rather than user-friendly description for location1`() {
    val location1 = Location(
      locationId = 1, locationType = "VIDE", description = "video-room-a", locationUsage = "APP",
      agencyId = "MDI", parentLocationId = 123, currentOccupancy = 2, locationPrefix = "MDI-prefix",
      operationalCapacity = 2, userDescription = null, internalLocationCode = "Room 1",
    )

    val location2 = Location(
      locationId = 2, locationType = "VIDE", description = "video-room-b", locationUsage = "APP",
      agencyId = "MDI", parentLocationId = 123, currentOccupancy = 2, locationPrefix = "MDI-prefix",
      operationalCapacity = 2, userDescription = "Video Room B", internalLocationCode = "Room 2",
    )

    whenever(prisonApiClient.getLocationsForTypeUnrestricted("MDI", "APP"))
      .thenReturn(Mono.just(listOf(location1, location2)))

    assertThat(locationService.getVideoLinkRoomsForPrison("MDI"))
      .isEqualTo(
        listOf(
          LocationIdAndDescription(locationId = 1, description = "video-room-a"),
          LocationIdAndDescription(locationId = 2, description = "Video Room B"),
        ),
      )
  }

  @Test
  fun `getCellLocationsForGroup - cells match predicate - returns cells`() {
    whenever(prisonApiClient.getLocationsForType("LEI", "CELL"))
      .thenReturn(Mono.just(listOf(cell1, cell2, cell3, cell4)))
    whenever(locationGroupService.locationGroupFilter("LEI", "mylist"))
      .thenReturn(locationPrefixPredicate("cell4", "cell1", "cell3"))

    val group = locationService.getCellLocationsForGroup("LEI", "mylist")

    assertThat(group).containsExactlyInAnyOrder(cell1, cell3, cell4)
  }

  @Test
  fun `getCellLocationsForGroup - descriptions need formatting - formatted correctly`() {
    val cell5 = aLocation(locationPrefix = "cell5", description = "yoi something")
    val cell6 = aLocation(locationPrefix = "cell6", description = "hmp something")

    whenever(prisonApiClient.getLocationsForType("LEI", "CELL"))
      .thenReturn(Mono.just(listOf(cell5, cell6)))
    whenever(locationGroupService.locationGroupFilter("LEI", "mylist"))
      .thenReturn(Predicate { true })

    val group = locationService.getCellLocationsForGroup("LEI", "mylist")

    assertThat(group).extracting("description").containsExactlyInAnyOrder("YOI Something", "HMP Something")
  }

  @Test
  fun `getCellLocationsForGroup - no cells match predicate - returns nothing`() {
    whenever(prisonApiClient.getLocationsForType("LEI", "CELL"))
      .thenReturn(Mono.just(listOf(cell1, cell2, cell3, cell4)))
    whenever(locationGroupService.locationGroupFilter("LEI", "mylist"))
      .thenReturn(Predicate { false })

    val group = locationService.getCellLocationsForGroup("LEI", "mylist")

    assertThat(group).isEmpty()
  }

  @Test
  fun `should throw entity not found error when trying to load the location prefix`() {
    whenever(groupsProperties.getProperty(anyString())).thenReturn(null)
    Assertions.assertThrows(EntityNotFoundException::class.java) {
      locationService.getLocationPrefixFromGroup("XXX", "1")
    }
  }

  @Test
  fun `should return location prefix for group`() {
    whenever(groupsProperties.getProperty(anyString())).thenReturn("MDI-2-")
    val locationPrefixDto = locationService.getLocationPrefixFromGroup("MDI", "Houseblock 7")
    assertThat(locationPrefixDto.locationPrefix).isEqualTo("MDI-2-")
  }

  private fun locationPrefixPredicate(vararg cells: String): Predicate<Location> {
    return listOf(*cells)
      .map { s -> Predicate { l: Location -> s == l.locationPrefix } }
      .reduce(Predicate<Location>::or)
  }

  private fun aLocation(locationPrefix: String, description: String = ""): Location {
    return Location(
      locationPrefix = locationPrefix,
      locationId = 0L,
      description = description,
      parentLocationId = null,
      userDescription = null,
      currentOccupancy = 0,
      operationalCapacity = 0,
      agencyId = "",
      internalLocationCode = "",
      locationUsage = "",
      locationType = "",
    )
  }
}

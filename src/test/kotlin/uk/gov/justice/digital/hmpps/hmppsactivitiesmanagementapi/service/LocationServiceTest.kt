package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api.LocationsInsidePrisonAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisMappingAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService.LocationDetails
import java.util.*
import java.util.function.Predicate

class LocationServiceTest {

  private val prisonApiClient: PrisonApiClient = mock()
  private val locationGroupService: LocationGroupService = mock()
  private val groupsProperties: Properties = mock()
  private val nomisMappingAPIClient: NomisMappingAPIClient = mock()
  private val locationsInsidePrisonAPIClient: LocationsInsidePrisonAPIClient = mock()

  private val locationService = LocationService(prisonApiClient, locationGroupService, groupsProperties, nomisMappingAPIClient, locationsInsidePrisonAPIClient)

  private val cell1 = aLocation(locationPrefix = "cell1")
  private val cell2 = aLocation(locationPrefix = "cell2")
  private val cell3 = aLocation(locationPrefix = "cell3")
  private val cell4 = aLocation(locationPrefix = "cell4")

  private val dpsLocationUuid = UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc")

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
  fun `should return location prefix for prisons using pattern for group`() {
    whenever(groupsProperties.getProperty(anyString())).thenReturn("MDI-2-")
    val locationPrefixDto = locationService.getLocationPrefixFromGroup("MDI", "Houseblock 7")
    assertThat(locationPrefixDto.locationPrefix).isEqualTo("MDI-2-")
  }

  @Test
  fun `should return location prefix for prisons using default for group`() {
    whenever(groupsProperties.getProperty(anyString())).thenReturn(null)
    val locationPrefixDto = locationService.getLocationPrefixFromGroup("LDI", "A_B")
    assertThat(locationPrefixDto.locationPrefix).isEqualTo("LDI-A-B-")
  }

  @Test
  fun `getLocationsForAppointments returns locations`() {
    whenever(prisonApiClient.getLocationsForTypeUnrestricted("TPR", "APP"))
      .thenReturn(Mono.just(listOf(appointmentLocation(1, "TPR"))))

    val locations = locationService.getLocationsForAppointments("TPR")

    assertThat(locations).containsExactly(appointmentLocation(1, "TPR"))
  }

  @Test
  fun `getLocationsForAppointmentsMap returns mapped locations`() {
    whenever(prisonApiClient.getLocationsForTypeUnrestricted("TPR", "APP"))
      .thenReturn(Mono.just(listOf(appointmentLocation(1, "TPR"), appointmentLocation(2, "TPR"))))

    val locations = locationService.getLocationsForAppointmentsMap("TPR")

    assertThat(locations).isEqualTo(mapOf(1L to appointmentLocation(1, "TPR"), 2L to appointmentLocation(2, "TPR")))
  }

  @Test
  fun `getLocationForSchedule - returns location details when local name is null`() {
    whenever(nomisMappingAPIClient.getLocationMappingByDpsId(dpsLocationUuid)).thenReturn(NomisDpsLocationMapping(dpsLocationUuid, 123))

    val dpsLocation = dpsLocation(localName = null)

    whenever(locationsInsidePrisonAPIClient.getLocationById(dpsLocationUuid)).thenReturn(dpsLocation)

    val result = locationService.getLocationForSchedule(dpsLocationUuid)

    assertThat(result).isEqualTo(LocationDetails(dpsLocation.prisonId, 123, dpsLocationUuid, dpsLocation.code, dpsLocation.code, dpsLocation.pathHierarchy))

    verify(nomisMappingAPIClient, never()).getLocationMappingByNomisId(any())
  }

  @Test
  fun `getLocationForSchedule - returns location details`() {
    whenever(nomisMappingAPIClient.getLocationMappingByDpsId(dpsLocationUuid)).thenReturn(NomisDpsLocationMapping(dpsLocationUuid, 123))

    val dpsLocation = dpsLocation()

    whenever(locationsInsidePrisonAPIClient.getLocationById(dpsLocationUuid)).thenReturn(dpsLocation)

    val result = locationService.getLocationForSchedule(dpsLocationUuid)

    assertThat(result).isEqualTo(LocationDetails(dpsLocation.prisonId, 123, dpsLocationUuid, dpsLocation.code, dpsLocation.localName!!, dpsLocation.pathHierarchy))

    verify(nomisMappingAPIClient, never()).getLocationMappingByNomisId(any())
  }

  private fun locationPrefixPredicate(vararg cells: String): Predicate<Location> = listOf(*cells)
    .map { s -> Predicate { l: Location -> s == l.locationPrefix } }
    .reduce(Predicate<Location>::or)

  private fun aLocation(locationPrefix: String, description: String = ""): Location = Location(
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

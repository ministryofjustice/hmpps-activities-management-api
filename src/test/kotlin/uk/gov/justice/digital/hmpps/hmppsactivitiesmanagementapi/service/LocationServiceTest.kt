package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api.LocationsInsidePrisonAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto.UsageType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisMappingAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
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
  fun `getLocationDetailsForAppointmentsMap returns DPS locations mapped by NOMIS locations ids`() {
    val dpsLocationId1 = UUID.randomUUID()
    val dpsLocationId2 = UUID.randomUUID()
    val dpsLocation1 = dpsLocation(dpsLocationId1)
    val dpsLocation2 = dpsLocation(dpsLocationId2)

    locationsInsidePrisonAPIClient.stub {
      on {
        runBlocking {
          locationsInsidePrisonAPIClient.getLocationsForUsageType("TPR", UsageType.APPOINTMENT)
        }
      } doReturn listOf(dpsLocation1, dpsLocation2)
    }

    nomisMappingAPIClient.stub {
      on {
        runBlocking {
          nomisMappingAPIClient.getLocationMappingsByDpsIds(setOf(dpsLocationId1, dpsLocationId2))
        }
      } doReturn listOf(
        NomisDpsLocationMapping(dpsLocationId1, 1),
        NomisDpsLocationMapping(dpsLocationId2, 2),
      )
    }

    val locations = locationService.getLocationDetailsForAppointmentsMap("TPR")

    val expectedLocations = mapOf<Long, LocationDetails>(
      1L to LocationDetails(dpsLocation1.prisonId, 1, dpsLocation1.id, dpsLocation1.code, dpsLocation1.localName!!, dpsLocation1.pathHierarchy),
      2L to LocationDetails(dpsLocation2.prisonId, 2, dpsLocation2.id, dpsLocation2.code, dpsLocation2.localName!!, dpsLocation2.pathHierarchy),
    )

    assertThat(locations).isEqualTo(expectedLocations)
  }

  @Test
  fun `getLocationDetailsForAppointmentsMapByDpsLocationId returns DPS locations mapped by DPS Location UUID`() {
    val dpsLocationId1 = UUID.randomUUID()
    val dpsLocationId2 = UUID.randomUUID()
    val dpsLocation1 = dpsLocation(dpsLocationId1)
    val dpsLocation2 = dpsLocation(dpsLocationId2)

    locationsInsidePrisonAPIClient.stub {
      on {
        runBlocking {
          locationsInsidePrisonAPIClient.getLocationsForUsageType("TPR", UsageType.APPOINTMENT)
        }
      } doReturn listOf(dpsLocation1, dpsLocation2)
    }

    nomisMappingAPIClient.stub {
      on {
        runBlocking {
          nomisMappingAPIClient.getLocationMappingsByDpsIds(setOf(dpsLocationId1, dpsLocationId2))
        }
      } doReturn listOf(
        NomisDpsLocationMapping(dpsLocationId1, 1),
        NomisDpsLocationMapping(dpsLocationId2, 2),
      )
    }

    val locations = locationService.getLocationDetailsForAppointmentsMapByDpsLocationId("TPR")

    val expectedLocations = mapOf<UUID, LocationDetails>(
      dpsLocationId1 to LocationDetails(dpsLocation1.prisonId, 1, dpsLocation1.id, dpsLocation1.code, dpsLocation1.localName!!, dpsLocation1.pathHierarchy),
      dpsLocationId2 to LocationDetails(dpsLocation2.prisonId, 2, dpsLocation2.id, dpsLocation2.code, dpsLocation2.localName!!, dpsLocation2.pathHierarchy),
    )

    assertThat(locations).isEqualTo(expectedLocations)
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

  @Test
  fun `location to appointment location summary returns a default description for null locations`() {
    assertThat((null as LocationDetails?).toAppointmentLocationSummary(1, dpsLocationUuid, "TPR")).isEqualTo(
      AppointmentLocationSummary(1, dpsLocationUuid, "TPR", "No information available"),
    )
  }

  @Test
  fun `location to appointment location summary mapping`() {
    assertThat(appointmentLocationDetails(1, dpsLocationUuid, "TPR").toAppointmentLocationSummary(1, dpsLocationUuid, "TPR")).isEqualTo(
      AppointmentLocationSummary(1, dpsLocationUuid, "TPR", "Test Appointment Location"),
    )
  }

  @Test
  fun `toAppointmentLocation returns a summary`() {
    val location1 = appointmentLocationDetails(1, UUID.randomUUID(), "TPR", "Chapel")
    val location2 = appointmentLocationDetails(2, UUID.randomUUID(), "MDI", "Kitchen")

    assertThat(listOf(location1, location2).toAppointmentLocation()).isEqualTo(
      listOf(
        AppointmentLocationSummary(1, location1.dpsLocationId, "TPR", "Chapel"),
        AppointmentLocationSummary(2, location2.dpsLocationId, "MDI", "Kitchen"),
      ),
    )
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

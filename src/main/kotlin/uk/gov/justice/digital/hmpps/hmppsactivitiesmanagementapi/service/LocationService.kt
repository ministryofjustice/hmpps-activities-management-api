package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.apache.commons.text.WordUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api.LocationsInsidePrisonAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisMappingAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixDto
import java.util.*

@Service
class LocationService(
  private val prisonApiClient: PrisonApiClient,
  @Qualifier("locationGroupServiceSelector") private val locationGroupService: LocationGroupService,
  @Qualifier("whereaboutsGroups") private val groupsProperties: Properties,
  private val nomisMappingAPIClient: NomisMappingAPIClient,
  private val locationsInsidePrisonAPIClient: LocationsInsidePrisonAPIClient,
) {

  fun getLocationPrefixFromGroup(agencyId: String, group: String): LocationPrefixDto {
    val agencyGroupKey = "${agencyId}_$group"
    val pattern = groupsProperties.getProperty(agencyGroupKey)

    val locationPrefix = pattern ?: "$agencyId-${group.replace('_', '-')}-"

    return LocationPrefixDto(locationPrefix)
  }

  fun getLocationsForAppointments(agencyId: String): List<Location> = prisonApiClient
    .getLocationsForTypeUnrestricted(agencyId, "APP").block() ?: emptyList()

  fun getLocationsForAppointmentsMap(agencyId: String): Map<Long, Location> = getLocationsForAppointments(agencyId)
    .associateBy { it.locationId }

  fun getCellLocationsForGroup(agencyId: String, groupName: String): List<Location>? = prisonApiClient.getLocationsForType(agencyId, "CELL").block()
    ?.filter(locationGroupService.locationGroupFilter(agencyId, groupName)::test)?.toMutableList()
    ?.map { it.copy(description = it.description.formatLocation()) }
    ?.toList()

  fun getLocationForSchedule(dpsLocationId: UUID): LocationDetails {
    // TODO: remove locationID later
    val locationID = nomisMappingAPIClient.getLocationMappingByDpsId(dpsLocationId)!!.nomisLocationId

    return locationsInsidePrisonAPIClient.getLocationById(dpsLocationId).let {
      // TODO: localName is optional - what should we use?
      LocationDetails(it.prisonId, locationID, it.id, it.code, it.localName ?: it.code)
    }
  }

  private fun String.formatLocation(): String = WordUtils.capitalizeFully(this)
    .replace(Regex("hmp|Hmp"), "HMP")
    .replace(Regex("yoi|Yoi"), "YOI")

  data class LocationDetails(val agencyId: String, val locationId: Long, val dpsLocationId: UUID, val internalLocationCode: String, val description: String?)
}

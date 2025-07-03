package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.apache.commons.text.WordUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api.LocationsInsidePrisonAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisMappingAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService.LocationDetails
import java.util.*
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.Location as DpsLocation

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

  fun getDpsLocationsForAppointments(agencyId: String): List<LocationDetails> = runBlocking {
    val locations = locationsInsidePrisonAPIClient.getLocationsForUsageType(agencyId, NonResidentialUsageDto.UsageType.APPOINTMENT)

    val mappings = getLocationMappingsByDpsIds(locations.toIdSet())

    locations.map {
      it.toLocationDetails(mappings[it.id]!!.nomisLocationId)
    }
  }

  fun getLocationMappingsByDpsIds(dpsLocationIds: Set<UUID>) = runBlocking {
    nomisMappingAPIClient.getLocationMappingsByDpsIds(dpsLocationIds).associateBy { it.dpsLocationId }
  }

  fun getLocationDetailsForAppointmentsMap(agencyId: String): Map<Long, LocationDetails> = getDpsLocationsForAppointments(agencyId)
    .associateBy { it.locationId }

  fun getLocationDetailsForAppointmentsMapByDpsLocationId(agencyId: String): Map<UUID, LocationDetails> = getDpsLocationsForAppointments(agencyId)
    .associateBy { it.dpsLocationId }

  fun getCellLocationsForGroup(agencyId: String, groupName: String): List<Location>? = prisonApiClient.getLocationsForType(agencyId, "CELL").block()
    ?.filter(locationGroupService.locationGroupFilter(agencyId, groupName)::test)?.toMutableList()
    ?.map { it.copy(description = it.description.formatLocation()) }
    ?.toList()

  fun getLocationForSchedule(dpsLocationId: UUID): LocationDetails {
    // TODO: remove locationID later
    val locationId = nomisMappingAPIClient.getLocationMappingByDpsId(dpsLocationId)!!.nomisLocationId

    return locationsInsidePrisonAPIClient.getLocationById(dpsLocationId).toLocationDetails(locationId)
  }

  private fun String.formatLocation(): String = WordUtils.capitalizeFully(this)
    .replace(Regex("hmp|Hmp"), "HMP")
    .replace(Regex("yoi|Yoi"), "YOI")

  data class LocationDetails(val agencyId: String, val locationId: Long, val dpsLocationId: UUID, val code: String, val description: String, val pathHierarchy: String? = null)
}

fun List<DpsLocation>.toIdSet() = this.map { it.id }.toSet()

fun DpsLocation.toLocationDetails(locationId: Long) = LocationDetails(this.prisonId, locationId, this.id, this.code, this.localName ?: this.code, this.pathHierarchy)

fun List<LocationDetails>.toMapByNomisId() = this.associateBy { it.locationId }

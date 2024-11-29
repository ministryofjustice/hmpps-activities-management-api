package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.apache.commons.text.WordUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixDto
import java.util.*

@Service
class LocationService(
  private val prisonApiClient: PrisonApiClient,
  @Qualifier("locationGroupServiceSelector") private val locationGroupService: LocationGroupService,
  @Qualifier("whereaboutsGroups") private val groupsProperties: Properties,
) {

  fun getLocationPrefixFromGroup(agencyId: String, group: String): LocationPrefixDto {
    val agencyGroupKey = "${agencyId}_$group"
    val pattern = groupsProperties.getProperty(agencyGroupKey)

    val locationPrefix = pattern ?: "$agencyId-${group.replace('_', '-')}-"

    return LocationPrefixDto(locationPrefix)
  }

  fun getLocationsForAppointments(agencyId: String): List<Location> =
    prisonApiClient
      .getLocationsForTypeUnrestricted(agencyId, "APP").block() ?: emptyList()

  fun getLocationsForAppointmentsMap(agencyId: String): Map<Long, Location> =
    getLocationsForAppointments(agencyId)
      .associateBy { it.locationId }

  fun getCellLocationsForGroup(agencyId: String, groupName: String): List<Location>? =
    prisonApiClient.getLocationsForType(agencyId, "CELL").block()
      ?.filter(locationGroupService.locationGroupFilter(agencyId, groupName)::test)?.toMutableList()
      ?.map { it.copy(description = it.description.formatLocation()) }
      ?.toList()

  private fun String.formatLocation(): String =
    WordUtils.capitalizeFully(this)
      .replace(Regex("hmp|Hmp"), "HMP")
      .replace(Regex("yoi|Yoi"), "YOI")
}

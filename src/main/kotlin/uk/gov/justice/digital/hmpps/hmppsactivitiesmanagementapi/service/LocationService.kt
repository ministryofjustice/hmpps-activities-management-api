package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.apache.commons.text.WordUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationIdAndDescription
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixDto
import java.util.Properties

@Service
class LocationService(
  private val prisonApiClient: PrisonApiClient,
  @Qualifier("locationGroupServiceSelector") private val locationGroupService: LocationGroupService,
  @Qualifier("whereaboutsGroups") private val groupsProperties: Properties,
) {

  fun getLocationPrefixFromGroup(agencyId: String, group: String): LocationPrefixDto {
    val agencyGroupKey = "${agencyId}_$group"
    val pattern = groupsProperties.getProperty(agencyGroupKey)
      ?: throw EntityNotFoundException("No location prefix found for prison $agencyId and group name '$group'")

    val locationPrefix = pattern
      .replace(".", "")
      .replace("+", "")

    return LocationPrefixDto(locationPrefix)
  }

  fun getLocationsForAppointments(agencyId: String): List<Location>? =
    prisonApiClient
      .getLocationsForTypeUnrestricted(agencyId, "APP").block()

  fun getLocationsForAppointmentsMap(agencyId: String, locationIds: List<Long?>) =
    getLocationsForAppointments(agencyId)
      ?.filter { locationIds.contains(it.locationId) }
      ?.associateBy { it.locationId }

  fun getVideoLinkRoomsForPrison(agencyId: String): List<LocationIdAndDescription>? =
    getLocationsForAppointments(agencyId)
      ?.filter { it.locationType == "VIDE" }
      ?.map { LocationIdAndDescription(it.locationId, it.userDescription ?: it.description) }

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

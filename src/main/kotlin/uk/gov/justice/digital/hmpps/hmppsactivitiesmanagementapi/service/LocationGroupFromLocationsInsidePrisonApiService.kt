package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api.LocationsInsidePrisonAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import java.util.function.Predicate

@Service("defaultLocationGroupService")
class LocationGroupFromLocationsInsidePrisonApiService(private val locationsInsidePrisonApiClient: LocationsInsidePrisonAPIClient) : LocationGroupService {

  override fun getLocationGroups(prisonCode: String): List<LocationGroup>? = runBlocking { locationsInsidePrisonApiClient.getLocationGroups(prisonCode) }

  override fun locationGroupFilter(prisonCode: String, groupName: String): Predicate<Location> {
    val prefixToMatch = "$prisonCode-${groupName.replace('_', '-')}-"
    return Predicate { it.locationPrefix?.startsWith(prefixToMatch) ?: false }
  }
}

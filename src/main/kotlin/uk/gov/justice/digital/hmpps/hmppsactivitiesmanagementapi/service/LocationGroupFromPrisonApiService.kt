package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import java.util.function.Predicate

@Service("defaultLocationGroupService")
class LocationGroupFromPrisonApiService(private val prisonApiClient: PrisonApiClient) : LocationGroupService {

  override fun getLocationGroups(agencyId: String): List<LocationGroup>? = prisonApiClient.getLocationGroups(agencyId).block()

  override fun locationGroupFilter(agencyId: String, groupName: String): Predicate<Location> {
    val prefixToMatch = "$agencyId-${groupName.replace('_', '-')}-"
    return Predicate { it.locationPrefix?.startsWith(prefixToMatch) ?: false }
  }
}

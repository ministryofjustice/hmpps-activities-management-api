package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import java.util.function.Predicate

@Service("locationGroupServiceSelector")
class LocationGroupServiceSelector(
  @Qualifier("defaultLocationGroupService") private val defaultService: LocationGroupService,
  @Qualifier("overrideLocationGroupService") private val overrideService: LocationGroupService
) : LocationGroupService {

  override fun getLocationGroups(agencyId: String): List<LocationGroup>? {
    val groups = overrideService.getLocationGroups(agencyId)
    return if (!groups.isNullOrEmpty()) {
      groups
    } else defaultService.getLocationGroups(agencyId)
  }

  override fun locationGroupFilter(agencyId: String, groupName: String): Predicate<Location> {
    return if (!overrideService.getLocationGroups(agencyId).isNullOrEmpty()) {
      overrideService.locationGroupFilter(agencyId, groupName)
    } else defaultService.locationGroupFilter(agencyId, groupName)
  }
}

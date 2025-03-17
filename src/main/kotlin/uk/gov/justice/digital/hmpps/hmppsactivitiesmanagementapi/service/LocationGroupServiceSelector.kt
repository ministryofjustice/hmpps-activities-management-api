package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import java.util.function.Predicate

@Service("locationGroupServiceSelector")
class LocationGroupServiceSelector(
  @Qualifier("defaultLocationGroupService") private val defaultService: LocationGroupService,
  @Qualifier("overrideLocationGroupService") private val overrideService: LocationGroupService,
  @Value("\${prison-locations.using-regex-config}") private val prisonsUsingRegexConfig: String,
) : LocationGroupService {

  override fun getLocationGroups(agencyId: String): List<LocationGroup>? {
    return if (isUsingRegexConfig(agencyId)) {
      overrideService.getLocationGroups(agencyId)
    } else {
      defaultService.getLocationGroups(agencyId)
    }
  }

  override fun locationGroupFilter(agencyId: String, groupName: String): Predicate<Location> = if (isUsingRegexConfig(agencyId)) {
    overrideService.locationGroupFilter(agencyId, groupName)
  } else {
    defaultService.locationGroupFilter(agencyId, groupName)
  }

  private fun isUsingRegexConfig(agencyId: String): Boolean {
    return prisonsUsingRegexConfig.split(",").contains(agencyId)
  }
}

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

  override fun getLocationGroups(prisonCode: String): List<LocationGroup> = if (isUsingRegexConfig(prisonCode)) {
    overrideService.getLocationGroups(prisonCode)
  } else {
    defaultService.getLocationGroups(prisonCode)
  }

  override fun locationGroupFilter(prisonCode: String, groupName: String): Predicate<Location> = if (isUsingRegexConfig(prisonCode)) {
    overrideService.locationGroupFilter(prisonCode, groupName)
  } else {
    defaultService.locationGroupFilter(prisonCode, groupName)
  }

  private fun isUsingRegexConfig(prisonCode: String): Boolean = prisonsUsingRegexConfig.split(",").contains(prisonCode)
}

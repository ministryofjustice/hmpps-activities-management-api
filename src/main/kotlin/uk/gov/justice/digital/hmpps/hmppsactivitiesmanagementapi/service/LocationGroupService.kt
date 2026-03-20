package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import java.util.function.Predicate

interface LocationGroupService {
  fun getLocationGroups(prisonCode: String): List<LocationGroup>
  fun locationGroupFilter(prisonCode: String, groupName: String): Predicate<Location>
}

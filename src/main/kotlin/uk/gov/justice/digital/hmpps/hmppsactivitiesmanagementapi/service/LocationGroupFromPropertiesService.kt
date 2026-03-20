package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import java.util.Properties
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * An implementation of LocationGroupService backed by a properties file.
 */
@Service("overrideLocationGroupService")
class LocationGroupFromPropertiesService(
  @Qualifier("whereaboutsGroups") private val groupsProperties: Properties,
) : LocationGroupService {

  /**
   * Return the set of Location Groups for a prisonCode, including any nested sub-groups.
   *
   * @param prisonCode The prison identifier
   * @return A list of LocationGroup, sorted by name, with each item containing its nested LocationGroups, also sorted by name.
   */
  override fun getLocationGroups(prisonCode: String): List<LocationGroup> {
    val fullKeys = groupsProperties.stringPropertyNames()
    return fullKeys.asSequence()
      .filter { it.startsWith(prisonCode) }
      .map { it.substring(prisonCode.length + 1) }
      .filterNot { it.contains("_") }
      .sorted()
      .map { LocationGroup(it, it, getAvailableSubGroups(prisonCode, it)) }
      .toList()
  }

  /**
   * Get the available sub-groups (sub-locations) for the named group/prisonCode.
   *
   * @param prisonCode  The prison identifier
   * @param groupName The name of a group
   * @return Alphabetically sorted List of subgroups matching the criteria
   */
  private fun getAvailableSubGroups(prisonCode: String, groupName: String): List<LocationGroup> {
    val fullKeys = groupsProperties.stringPropertyNames()
    val prisonCodeAndGroupName = "${prisonCode}_${groupName}_"
    return fullKeys.asSequence()
      .filter { it.startsWith(prisonCodeAndGroupName) }
      .map { it.substring(prisonCodeAndGroupName.length) }
      .sorted()
      .map { LocationGroup(it, it, emptyList()) }
      .toList()
  }

  override fun locationGroupFilter(prisonCode: String, groupName: String): Predicate<Location> {
    val patterns = groupsProperties.getProperty("${prisonCode}_$groupName")
      ?: throw EntityNotFoundException("Group $groupName does not exist for prisonCode $prisonCode.")
    val patternStrings = patterns.split(",")
    return patternStrings.asSequence()
      .map(Pattern::compile)
      .map { pattern -> Predicate { l: Location -> pattern.matcher(l.locationPrefix.toString()).matches() } }
      .reduce(Predicate<Location>::or)
  }
}

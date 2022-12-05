package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup

class LocationGroupServiceSelectorTest {

  private val defaultService: LocationGroupService = mock()
  private val overrideService: LocationGroupService = mock()
  private var service: LocationGroupService = LocationGroupServiceSelector(defaultService, overrideService)

  @Test
  fun locationGroupsCallsDefaultWhenNoOverride() {
    whenever(defaultService.getLocationGroups("LEI")).thenReturn(listOf(LG1))
    assertThat(service.getLocationGroups("LEI")).contains(LG1)
    verify(overrideService).getLocationGroups("LEI")
  }

  @Test
  fun locationGroupsDoesNotCallDefaultWhenOverridden() {
    whenever(overrideService.getLocationGroups("LEI")).thenReturn(listOf(LG1))
    assertThat(service.getLocationGroups("LEI")).contains(LG1)
    verify(overrideService).getLocationGroups("LEI")
    verifyNoMoreInteractions(defaultService)
  }

  @Test
  fun locationGroupsForAgencyDelegatesToGetLocationGroups() {
    whenever(defaultService.getLocationGroups("LEI")).thenReturn(listOf(LG1))
    assertThat(service.getLocationGroups("LEI")).contains(LG1)
    verify(overrideService).getLocationGroups("LEI")
  }

  @Test
  fun locationGroupsFiltersCallsDefaultWhenNoOverride() {
    service.locationGroupFilter("LEI", "Z")
    verify(defaultService).locationGroupFilter("LEI", "Z")
  }

  @Test
  fun locationGroupsFiltersCallsOverrideOnlyIfOverridden() {
    whenever(overrideService.getLocationGroups("LEI")).thenReturn(listOf(LG1))
    service.locationGroupFilter("LEI", "Z")
    verify(overrideService).locationGroupFilter("LEI", "Z")
    verifyNoMoreInteractions(defaultService)
  }

  companion object {
    private val LG1 = LocationGroup(key = "A", name = "A", children = emptyList())
  }
}

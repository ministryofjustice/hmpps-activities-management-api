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
  private var service: LocationGroupService = LocationGroupServiceSelector(defaultService, overrideService, "RSI")

  @Test
  fun locationGroupsCallsDefaultWhenRegexNotConfigured() {
    whenever(defaultService.getLocationGroups("LEI")).thenReturn(listOf(LG1))
    assertThat(service.getLocationGroups("LEI")).contains(LG1)
    verify(defaultService).getLocationGroups("LEI")
    verifyNoMoreInteractions(overrideService)
  }

  @Test
  fun locationGroupsCallOverrideWhenRegexConfigExist() {
    whenever(overrideService.getLocationGroups("RSI")).thenReturn(listOf(LG1))
    assertThat(service.getLocationGroups("RSI")).contains(LG1)
    verify(overrideService).getLocationGroups("RSI")
    verifyNoMoreInteractions(defaultService)
  }

  @Test
  fun locationGroupsFiltersCallsDefaultRegexNotConfigured() {
    whenever(defaultService.getLocationGroups("LEI")).thenReturn(listOf(LG1))
    service.locationGroupFilter("LEI", "Z")
    verify(defaultService).locationGroupFilter("LEI", "Z")
    verifyNoMoreInteractions(overrideService)
  }

  @Test
  fun locationGroupsFiltersCallsOverrideWhenRegexConfigExist() {
    whenever(overrideService.getLocationGroups("RSI")).thenReturn(listOf(LG1))
    service.locationGroupFilter("RSI", "A")
    verify(overrideService).locationGroupFilter("RSI", "A")
    verifyNoMoreInteractions(defaultService)
  }

  companion object {
    private val LG1 = LocationGroup(key = "A", name = "A", children = emptyList())
  }
}

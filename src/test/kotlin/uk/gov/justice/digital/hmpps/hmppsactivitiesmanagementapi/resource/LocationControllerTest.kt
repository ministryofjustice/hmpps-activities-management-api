package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationGroupServiceSelector
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService

@WebMvcTest(controllers = [LocationController::class])
@ContextConfiguration(classes = [LocationController::class])
class LocationControllerTest : ControllerTestBase<LocationController>() {

  @MockBean
  private lateinit var locationService: LocationService

  @MockBean
  private lateinit var locationGroupServiceSelector: LocationGroupServiceSelector

  private val groupName = "Houseblock 1"
  private val prisonCode = "MDI"

  override fun controller() = LocationController(locationService, locationGroupServiceSelector)

  @Test
  fun `Cell locations for group - 200 response when locations found`() {
    val cells = listOf(aLocation(groupName, "cell1"), aLocation(groupName, "cell2"))

    whenever(locationService.getCellLocationsForGroup(prisonCode, groupName)).thenReturn(cells)

    val response = mockMvc.get("/locations/prison/$prisonCode") {
      param("groupName", groupName)
    }
      .andExpect {
        status { isOk() }
        content { contentType(MediaType.APPLICATION_JSON) }
      }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(cells))
    verify(locationService).getCellLocationsForGroup(prisonCode, groupName)
  }

  @Test
  fun `Cell locations for group - 400 response when group name missing`() {
    mockMvc.get("/locations/prison/$prisonCode")
      .andDo { print() }
      .andExpect {
        status { is4xxClientError() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.userMessage") {
            value("Required request parameter 'groupName' for method parameter type String is not present")
          }
        }
      }
  }

  @Test
  fun `Cell locations for group - Error response when service throws exception`() {
    val result = this::class.java.getResource("/__files/error-500.json")?.readText()

    whenever(locationService.getCellLocationsForGroup(prisonCode, groupName)).thenThrow(RuntimeException("Error"))

    val response = mockMvc.get("/locations/prison/$prisonCode") {
      param("groupName", groupName)
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { is5xxServerError() } }
      .andReturn().response

    assertThat(response.contentAsString + "\n").isEqualTo(result)
    verify(locationService).getCellLocationsForGroup(prisonCode, groupName)
  }

  @Test
  fun `Location groups - 200 response when found`() {
    val result = listOf(LocationGroup(key = "A", name = "A", children = emptyList()))

    whenever(locationGroupServiceSelector.getLocationGroups(prisonCode)).thenReturn(result)

    val response = mockMvc.get("/locations/prison/$prisonCode/location-groups")
      .andExpect {
        status { isOk() }
        content { contentType(MediaType.APPLICATION_JSON) }
      }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))
    verify(locationGroupServiceSelector).getLocationGroups(prisonCode)
  }

  @Test
  fun `Location groups - Error response when service throws exception`() {
    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    whenever(
      locationGroupServiceSelector.getLocationGroups(prisonCode)
    ).thenThrow(RuntimeException("Error"))

    val response = mockMvc.get("/locations/prison/$prisonCode/location-groups")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { is5xxServerError() } }
      .andReturn().response

    assertThat(response.contentAsString + "\n").isEqualTo(result)
    verify(locationGroupServiceSelector).getLocationGroups(prisonCode)
  }

  @Test
  fun `Location prefix - 200 response when found`() {
    val result = LocationPrefixDto("MDI-2-")

    whenever(locationService.getLocationPrefixFromGroup(prisonCode, groupName)).thenReturn(result)

    val response = mockMvc.get("/locations/prison/$prisonCode/location-prefix") {
      param("groupName", groupName)
    }
      .andExpect {
        status { isOk() }
        content { contentType(MediaType.APPLICATION_JSON) }
      }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))
    verify(locationService).getLocationPrefixFromGroup(prisonCode, groupName)
  }

  @Test
  fun `Location prefix - 404 response when prefix not found`() {
    whenever(locationService.getLocationPrefixFromGroup(prisonCode, groupName))
      .thenThrow(EntityNotFoundException("Not found"))

    val response = mockMvc.get("/locations/prison/$prisonCode/location-prefix") {
      param("groupName", groupName)
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")
    verify(locationService).getLocationPrefixFromGroup(prisonCode, groupName)
  }

  @Test
  fun `Location prefix - Error response when service throws exception`() {
    val result = this::class.java.getResource("/__files/error-500.json")?.readText()

    whenever(locationService.getLocationPrefixFromGroup(prisonCode, groupName)).thenThrow(RuntimeException("Error"))

    val response = mockMvc.get("/locations/prison/$prisonCode/location-prefix") {
      param("groupName", groupName)
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { is5xxServerError() } }
      .andReturn().response

    assertThat(response.contentAsString + "\n").isEqualTo(result)
    verify(locationService).getLocationPrefixFromGroup(prisonCode, groupName)
  }

  @Test
  fun `Location prefix - 400 response when groupName request parameter is missing`() {
    mockMvc.get("/locations/prison/$prisonCode/location-prefix")
      .andDo { print() }
      .andExpect {
        status { is4xxClientError() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.userMessage") {
            value("Required request parameter 'groupName' for method parameter type String is not present")
          }
        }
      }
  }

  private fun aLocation(locationPrefix: String, description: String = ""): Location {
    return Location(
      locationPrefix = locationPrefix,
      locationId = 0L,
      description = description,
      parentLocationId = null,
      userDescription = null,
      currentOccupancy = 0,
      operationalCapacity = 0,
      agencyId = "",
      internalLocationCode = "",
      locationUsage = "",
      locationType = ""
    )
  }
}

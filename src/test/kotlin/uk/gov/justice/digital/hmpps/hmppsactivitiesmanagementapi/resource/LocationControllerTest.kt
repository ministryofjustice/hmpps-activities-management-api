package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationGroupServiceSelector
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService

@WebMvcTest(controllers = [LocationGroupController::class])
@ContextConfiguration(classes = [LocationGroupController::class])
class LocationControllerTest : ControllerTestBase<LocationController>() {

  @MockBean
  private lateinit var locationService: LocationService

  @MockBean
  private lateinit var locationGroupServiceSelector: LocationGroupServiceSelector

  override fun controller() = LocationController(locationService)

  @Test
  fun `200 response when locations found`() {
    val cell1 = aLocation(locationPrefix = "Houseblock 7", description = "cell1 something")
    val cell2 = aLocation(locationPrefix = "Houseblock 7", description = "cell2 something")
    whenever(
      locationService.getCellLocationsForGroup("MDI", "Houseblock 7")
    ).thenReturn(listOf(cell1, cell2))

    val response = mockMvc.get("/prisons/MDI/locations") {
      param("groupName", "Houseblock 7")
    }
      .andExpect {
        status { isOk() }
        content { contentType(MediaType.APPLICATION_JSON) }
      }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(cell1, cell2)))
    verify(locationService).getCellLocationsForGroup("MDI", "Houseblock 7")
  }

  @Test
  fun `400 response when group name missing`() {
    mockMvc.get("/prisons/MDI/locations")
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
  fun `Error response when service throws exception`() {
    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    whenever(
      locationService.getCellLocationsForGroup("MDI", "Houseblock 7")
    ).thenThrow(RuntimeException("Error"))

    val response = mockMvc.get("/prisons/MDI/locations") {
      param("groupName", "Houseblock 7")
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { is5xxServerError() } }
      .andReturn().response

    assertThat(response.contentAsString + "\n").isEqualTo(result)

    verify(locationService).getCellLocationsForGroup("MDI", "Houseblock 7")
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

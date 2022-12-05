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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService
import javax.persistence.EntityNotFoundException

@WebMvcTest(controllers = [LocationPrefixController::class])
@ContextConfiguration(classes = [LocationPrefixController::class])
class LocationPrefixControllerTest : ControllerTestBase<LocationPrefixController>() {

  @MockBean
  private lateinit var locationService: LocationService

  override fun controller() = LocationPrefixController(locationService)

  @Test
  fun `200 response when location prefix found`() {
    val result = LocationPrefixDto("MDI-2-")
    whenever(
      locationService.getLocationPrefixFromGroup("MDI", "Houseblock 7")
    ).thenReturn(result)

    val response = mockMvc.get("/prisons/MDI/location-prefix") {
      param("groupName", "Houseblock 7")
    }
      .andExpect {
        status { isOk() }
        content { contentType(MediaType.APPLICATION_JSON) }
      }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))
    verify(locationService).getLocationPrefixFromGroup(
      "MDI", "Houseblock 7",
    )
  }

  @Test
  fun `404 response when location prefix not found`() {
    whenever(
      locationService.getLocationPrefixFromGroup("MDI", "Houseblock 7")
    ).thenThrow(EntityNotFoundException("Not found"))

    val response = mockMvc.get("/prisons/MDI/location-prefix") {
      param("groupName", "Houseblock 7")
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Not found")
    verify(locationService).getLocationPrefixFromGroup(
      "MDI", "Houseblock 7",
    )
  }

  @Test
  fun `Error response when service throws exception`() {
    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    whenever(
      locationService.getLocationPrefixFromGroup("MDI", "Houseblock 7")
    ).thenThrow(RuntimeException("Error"))

    val response = mockMvc.get("/prisons/MDI/location-prefix") {
      param("groupName", "Houseblock 7")
    }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { is5xxServerError() } }
      .andReturn().response

    assertThat(response.contentAsString + "\n").isEqualTo(result)
    verify(locationService).getLocationPrefixFromGroup(
      "MDI", "Houseblock 7",
    )
  }

  @Test
  fun `400 response when group name missing`() {
    mockMvc.get("/prisons/MDI/location-prefix")
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
}

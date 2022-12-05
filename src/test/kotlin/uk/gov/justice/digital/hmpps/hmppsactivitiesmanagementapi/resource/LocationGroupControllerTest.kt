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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationGroupServiceSelector

@WebMvcTest(controllers = [LocationGroupController::class])
@ContextConfiguration(classes = [LocationGroupController::class])
class LocationGroupControllerTest : ControllerTestBase<LocationGroupController>() {

  @MockBean
  private lateinit var locationGroupServiceSelector: LocationGroupServiceSelector

  override fun controller() = LocationGroupController(locationGroupServiceSelector)

  @Test
  fun `200 response when location groups found`() {
    val result = listOf(LocationGroup(key = "A", name = "A", children = emptyList()))
    whenever(
      locationGroupServiceSelector.getLocationGroups("MDI")
    ).thenReturn(result)

    val response = mockMvc.get("/prisons/MDI/location-groups")
      .andExpect {
        status { isOk() }
        content { contentType(MediaType.APPLICATION_JSON) }
      }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))
    verify(locationGroupServiceSelector).getLocationGroups("MDI")
  }

  @Test
  fun `Error response when service throws exception`() {
    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    whenever(
      locationGroupServiceSelector.getLocationGroups("MDI")
    ).thenThrow(RuntimeException("Error"))

    val response = mockMvc.get("/prisons/MDI/location-groups")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { is5xxServerError() } }
      .andReturn().response

    assertThat(response.contentAsString + "\n").isEqualTo(result)

    verify(locationGroupServiceSelector).getLocationGroups("MDI")
  }
}

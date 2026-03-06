package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.internalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ModelTest.Companion.objectMapper
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.LocationPrefixesRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixesDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.InternalLocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationGroupServiceSelector
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService
import java.time.LocalDate

@WebMvcTest(controllers = [LocationController::class])
@ContextConfiguration(classes = [LocationController::class])
class LocationControllerTest : ControllerTestBase() {

  @MockitoBean
  private lateinit var locationService: LocationService

  @MockitoBean
  private lateinit var locationGroupServiceSelector: LocationGroupServiceSelector

  @MockitoBean
  private lateinit var internalLocationService: InternalLocationService

  private val groupName = "Houseblock 1"
  private val prisonCode = "MDI"

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
      locationGroupServiceSelector.getLocationGroups(prisonCode),
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
  fun `should return 200 when location prefixes are found`() {
    val request = LocationPrefixesRequest(listOf("North All", "North Landing 1"))
    val expectedResponse = listOf(
      LocationPrefixesDto("North All", "RSI-A-N-.+"),
      LocationPrefixesDto("North Landing 1", "RSI-A-N-1-.+"),
    )

    whenever(locationService.getLocationPrefixesFromGroup("RSI", "A-Wing", request))
      .thenReturn(expectedResponse)

    val response = mockMvc.post("/locations/prison/RSI/location-prefix") {
      content = objectMapper.writeValueAsString(request)
      contentType = MediaType.APPLICATION_JSON
      param("locationKey", "A-Wing")
    }.andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))
    verify(locationService).getLocationPrefixesFromGroup("RSI", "A-Wing", request)
  }

  @Test
  fun `should return 400 when the sub-locations list is empty`() {
    val request = LocationPrefixesRequest(emptyList())

    mockMvc.post("/locations/prison/RSI/location-prefix") {
      content = objectMapper.writeValueAsString(request)
      contentType = MediaType.APPLICATION_JSON
      param("locationKey", "A-Wing")
    }.andExpect { status { isBadRequest() } }

    verify(locationService, never()).getLocationPrefixesFromGroup(any(), any(), any())
  }

  @Test
  @WithAnonymousUser
  fun `should return 401 when the user is unauthorised`() {
    val request = LocationPrefixesRequest(listOf("North All"))

    mockMvc.post("/locations/prison/RSI/location-prefix") {
      content = objectMapper.writeValueAsString(request)
      contentType = MediaType.APPLICATION_JSON
      param("locationKey", "A-Wing")
    }.andExpect { status { isUnauthorized() } }

    verify(locationService, never()).getLocationPrefixesFromGroup(any(), any(), any())
  }

  @Test
  @WithMockUser(roles = ["INVALID_ROLE"])
  fun `should return 403 when the user has an invalid role`() {
    val request = LocationPrefixesRequest(listOf("North All"))

    mockMvc.post("/locations/prison/RSI/location-prefix") {
      content = objectMapper.writeValueAsString(request)
      contentType = MediaType.APPLICATION_JSON
      param("locationKey", "A-Wing")
    }.andExpect { status { isForbidden() } }

    verify(locationService, never()).getLocationPrefixesFromGroup(any(), any(), any())
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

  @Test
  fun `Internal location events summaries - 200 response when internal locations with events found`() {
    val date = LocalDate.now()
    val timeSlot = TimeSlot.AM
    val locations = setOf(internalLocationEventsSummary())

    internalLocationService.stub {
      on {
        runBlocking {
          internalLocationService.getInternalLocationEventsSummaries(prisonCode, date, timeSlot)
        }
      } doReturn locations
    }

    val response = mockMvc.getInternalLocationEventsSummaries(prisonCode, date, timeSlot)
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(locations))
  }

  @Test
  fun `Internal location events summaries - 200 response when no time slot supplied`() {
    val date = LocalDate.now()
    val locations = setOf(internalLocationEventsSummary())

    internalLocationService.stub {
      on {
        runBlocking {
          internalLocationService.getInternalLocationEventsSummaries(prisonCode, date, null)
        }
      } doReturn locations
    }

    val response = mockMvc.getInternalLocationEventsSummaries(prisonCode, date, null)
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(locations))
  }

  @Test
  fun `Internal location events summaries - 400 response when no date supplied`() {
    mockMvc.get("/locations/prison/$prisonCode/events-summaries")
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Required request parameter 'date' for method parameter type LocalDate is not present")
          }
        }
      }

    verifyNoInteractions(internalLocationService)
  }

  @Test
  fun `Internal location events summaries - 400 response when invalid date supplied`() {
    mockMvc.get("/locations/prison/$prisonCode/events-summaries?date=invalid")
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Error converting 'date' (invalid): Method parameter 'date': Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
          }
        }
      }

    verifyNoInteractions(internalLocationService)
  }

  @Test
  fun `Internal location events summaries - 400 response when invalid time slot supplied`() {
    val date = LocalDate.now()
    mockMvc.get("/locations/prison/$prisonCode/events-summaries?date=$date&timeSlot=no")
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Error converting 'timeSlot' (no): Method parameter 'timeSlot': Failed to convert value of type 'java.lang.String' to required type 'uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot'")
          }
        }
      }

    verifyNoInteractions(internalLocationService)
  }

  @Test
  fun `Internal location events summaries - 500 response when service throws exception`() {
    val date = LocalDate.now()

    internalLocationService.stub {
      on {
        runBlocking {
          internalLocationService.getInternalLocationEventsSummaries(prisonCode, date, null)
        }
      } doThrow RuntimeException("Error")
    }

    val response = mockMvc.getInternalLocationEventsSummaries(prisonCode, date, null)
      .andExpect { status { isInternalServerError() } }
      .andReturn().response

    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    assertThat(response.contentAsString + "\n").isEqualTo(result)
  }

  private fun aLocation(locationPrefix: String, description: String = ""): Location = Location(
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
    locationType = "",
  )

  private fun MockMvc.getInternalLocationEventsSummaries(
    prisonCode: String,
    date: LocalDate?,
    timeSlot: TimeSlot? = null,
  ) = get("/locations/prison/$prisonCode/events-summaries?date=$date" + (timeSlot?.let { "&timeSlot=$timeSlot" } ?: "")) {
    accept = MediaType.APPLICATION_JSON
    contentType = MediaType.APPLICATION_JSON
  }.andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }

  private fun MockMvc.getLocationPrefixes(
    prisonCode: String,
    locationKey: String,
    request: LocationPrefixesRequest,
  ) = post("/locations/prison/$prisonCode/location-prefix") {
    content = objectMapper.writeValueAsString(request)
    contentType = MediaType.APPLICATION_JSON
    param("locationKey", locationKey)
  }
}

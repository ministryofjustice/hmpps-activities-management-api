package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON

class LocationIntegrationTest : IntegrationTestBase() {

  @Test
  fun `locations by group name - defined in properties - selects relevant locations only`() {
    val prisonCode = "RNI"
    val groupName = "House block 7"

    prisonApiMockServer.stubGetLocationsForType("RNI", "CELL", "prisonapi/locations-RNI-HB7.json")
    val result = this::class.java.getResource("/__files/prisonapi/RNI_location_groups_agency_locname.json")?.readText()

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(result!!)
  }

  @Test
  fun `locations by group name - defined in prison API - selects relevant locations only`() {
    val prisonCode = "LEI"
    val groupName = "House_block_7"

    prisonApiMockServer.stubGetLocationsForType("LEI", "CELL", "prisonapi/locations-LEI-HB7.json")
    val result = this::class.java.getResource("/__files/prisonapi/LEI_location_groups_agency_locname.json")?.readText()

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(result!!)
  }

  @Test
  fun `location groups by name - agency locations not found - returns not found`() {
    val prisonCode = "not_an_agency"
    val groupName = "House block 7"

    prisonApiMockServer.stubGetLocationsForTypeNotFound(prisonCode, "CELL")

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Not Found")
  }

  @Test
  fun `location groups by name - error from prison API - server error passed to client`() {
    val prisonCode = "any_agency"
    val groupName = "any_location_type"

    prisonApiMockServer.stubGetLocationsForTypeServerError(prisonCode, "CELL")

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().is5xxServerError
      .expectBody()
      .jsonPath("$.userMessage")
      .isEqualTo("Internal Server Error")
  }

  @Test
  fun `location groups - success - none in properties so should fetch from prison API`() {
    val result = this::class.java.getResource("/__files/prisonapi/LEI_location_groups.json")?.readText()
    val prisonCode = "LEI"

    prisonApiMockServer.stubGetLocationGroups(prisonCode, "prisonapi/LEI_location_groups.json")

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}/location-groups")
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(result!!)
  }

  @Test
  fun `get location groups - success - exist in properties file`() {
    val result = this::class.java.getResource("/__files/prisonapi/location-groups-2.json")?.readText()
    val prisonCode = "MDI"

    prisonApiMockServer.stubGetLocationGroups(prisonCode, "prisonapi/location-groups-1.json")

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}/location-groups")
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(result!!)
  }

  @Test
  fun `get location groups - not found`() {
    val prisonCode = "XXX"

    prisonApiMockServer.stubGetLocationGroupsNotFound(prisonCode)

    val errorResponse = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}/location-groups")
          .build(prisonCode)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(errorResponse!!) {
      assertThat(errorCode).isNull()
      assertThat(moreInfo).isNull()
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("Not Found")
    }
  }

  @Test
  fun `get location prefix by group pattern`() {
    assertThat(webTestClient.getLocationPrefix("MDI", "Houseblock 1"))
      .isEqualTo(LocationPrefixDto("MDI-1-.+"))
  }

  @Test
  fun `get location prefix by group default`() {
    assertThat(webTestClient.getLocationPrefix("LDI", "A_B"))
      .isEqualTo(LocationPrefixDto("LDI-A-B-"))
  }

  private fun WebTestClient.getLocationPrefix(prisonCode: String, groupName: String) =
    get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}/location-prefix")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody(LocationPrefixDto::class.java)
      .returnResult().responseBody
}

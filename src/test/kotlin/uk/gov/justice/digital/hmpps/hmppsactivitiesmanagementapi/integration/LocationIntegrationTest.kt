package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse

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
      .headers(setAuthorisation(roles = listOf("ROLE_LICENCE_CA", "ROLE_KW_ADMIN")))
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
      .headers(setAuthorisation(roles = listOf("ROLE_LICENCE_CA", "ROLE_KW_ADMIN")))
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
      .headers(setAuthorisation(roles = listOf("ROLE_LICENCE_CA", "ROLE_KW_ADMIN")))
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.developerMessage")
      .isEqualTo("(developer message)Locations not found for agency not_an_agency with location type CELL")
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
      .headers(setAuthorisation(roles = listOf("ROLE_LICENCE_CA", "ROLE_KW_ADMIN")))
      .exchange()
      .expectStatus().is5xxServerError
      .expectBody()
      .jsonPath("$.developerMessage")
      .isEqualTo("Error")
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
      .headers(setAuthorisation(roles = listOf("ROLE_LICENCE_CA", "ROLE_KW_ADMIN")))
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
      .headers(setAuthorisation(roles = listOf("ROLE_LICENCE_CA", "ROLE_KW_ADMIN")))
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
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(errorResponse!!) {
      assertThat(errorCode).isNull()
      assertThat(developerMessage).isEqualTo("(developer message)Location groups not found for prison $prisonCode")
      assertThat(moreInfo).isNull()
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("(user message)Location groups not found for prison $prisonCode")
    }
  }

  @Test
  fun `get location prefix by group`() {
    val prisonCode = "MDI"
    val groupName = "Houseblock 1"

    val result = this::class.java.getResource("/__files/prisonapi/MDI_location-prefix.json")?.readText()

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}/location-prefix")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf("ROLE_LICENCE_CA", "ROLE_KW_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(result!!)
  }

  @Test
  fun `get location prefix by group - 404 if no prefix found`() {
    val prisonCode = "MDI"
    val groupName = "IDONTEXIST"

    val errorResponse = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}/location-prefix")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(errorResponse!!) {
      assertThat(errorCode).isNull()
      assertThat(developerMessage).isEqualTo("No location prefix found for prison $prisonCode and group name '$groupName'")
      assertThat(moreInfo).isNull()
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("Not found: No location prefix found for prison $prisonCode and group name '$groupName'")
    }
  }
}

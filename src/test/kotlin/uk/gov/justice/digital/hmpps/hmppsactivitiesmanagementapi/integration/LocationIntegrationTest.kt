package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.springframework.web.util.UriBuilder

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
          .path("/prisons/{prisonCode}/locations")
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
          .path("/prisons/{prisonCode}/locations")
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
  fun `location groups for agency by location name - agency locations not found - returns not found`() {
    val prisonCode = "not_an_agency"
    val groupName = "House block 7"
    prisonApiMockServer.stubGetLocationsForTypeNotFound("not_an_agency", "CELL")

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisons/{prisonCode}/locations")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf("ROLE_LICENCE_CA", "ROLE_KW_ADMIN")))
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.developerMessage").isEqualTo("(developer message)Locations not found for agency not_an_agency with location type CELL")
  }

  @Test
  fun `location groups for agency by location name - server error from prison API - server error passed to client`() {
    val prisonCode = "any_agency"
    val groupName = "any_location_type"
    prisonApiMockServer.stubGetLocationsForTypeServerError("any_agency", "CELL")

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisons/{prisonCode}/locations")
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
}

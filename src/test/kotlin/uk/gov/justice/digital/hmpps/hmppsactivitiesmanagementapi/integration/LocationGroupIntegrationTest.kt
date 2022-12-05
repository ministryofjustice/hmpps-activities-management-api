package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse

class LocationGroupIntegrationTest : IntegrationTestBase() {

  @Test
  fun `get location groups - success - none in properties so should fetch from prison API`() {
    val result = this::class.java.getResource("/__files/prisonapi/LEI_location_groups.json")?.readText()
    val prisonCode = "LEI"
    prisonApiMockServer.stubGetLocationGroups(prisonCode, "prisonapi/LEI_location_groups.json")

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisons/{prisonCode}/location-groups")
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf("ROLE_LICENCE_CA", "ROLE_KW_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(result!!)
  }

  @Test
  fun `get location groups - success - exist in properties`() {
    val result = this::class.java.getResource("/__files/prisonapi/location-groups-2.json")?.readText()
    val prisonCode = "MDI"
    prisonApiMockServer.stubGetLocationGroups(prisonCode, "prisonapi/location-groups-1.json")

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisons/{prisonCode}/location-groups")
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
          .path("/prisons/{prisonCode}/location-groups")
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
      assertThat(developerMessage).isEqualTo("(developer message)Location groups not found for prison XXX")
      assertThat(moreInfo).isNull()
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("(user message)Location groups not found for prison XXX")
    }
  }
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse

class LocationPrefixIntegrationTest : IntegrationTestBase() {

  @Test
  fun `get location prefix by group`() {
    val result = this::class.java.getResource("/__files/prisonapi/MDI_location-prefix.json")?.readText()
    val prisonCode = "MDI"
    val groupName = "Houseblock 1"

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/prisons/{prisonCode}/location-prefix")
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
          .path("/prisons/{prisonCode}/location-prefix")
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
      Assertions.assertThat(errorCode).isNull()
      Assertions.assertThat(developerMessage).isEqualTo("No location prefix found for prison MDI and group name 'IDONTEXIST'")
      Assertions.assertThat(moreInfo).isNull()
      Assertions.assertThat(status).isEqualTo(404)
      Assertions.assertThat(userMessage).isEqualTo("Not found: No location prefix found for prison MDI and group name 'IDONTEXIST'")
    }
  }
}

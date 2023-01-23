package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand

class PrisonPayBandIntegrationTest : IntegrationTestBase() {

  @Test
  fun `10 default pay bands are returned for Pentonville`() {
    val defaultPayBands = webTestClient.getPrisonPayBands(pentonvillePrisonCode)!!

    assertThat(defaultPayBands).hasSize(10)
    assertThat(defaultPayBands.count { it.prisonCode == "DEFAULT" }).isEqualTo(10)
  }

  @Test
  fun `3 configured pay bands are returned for Moorland`() {
    val defaultPayBands = webTestClient.getPrisonPayBands(moorlandPrisonCode)!!

    assertThat(defaultPayBands).hasSize(3)
    assertThat(defaultPayBands.count { it.prisonCode == moorlandPrisonCode }).isEqualTo(3)
  }

  private fun WebTestClient.getPrisonPayBands(prisonCode: String) =
    get()
      .uri("/prison/$prisonCode/prisonPayBands")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(PrisonPayBand::class.java)
      .returnResult().responseBody
}

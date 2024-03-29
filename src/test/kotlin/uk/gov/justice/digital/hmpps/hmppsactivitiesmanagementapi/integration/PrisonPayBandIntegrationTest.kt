package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON

class PrisonPayBandIntegrationTest : IntegrationTestBase() {

  @Test
  fun `10 configured pay bands are returned for Pentonville`() {
    val defaultPayBands = webTestClient.getPrisonPayBands(PENTONVILLE_PRISON_CODE)!!

    assertThat(defaultPayBands).hasSize(10)
    assertThat(defaultPayBands.count { it.prisonCode == "PVI" }).isEqualTo(10)
  }

  @Test
  fun `3 configured pay bands are returned for Moorland`() {
    val defaultPayBands = webTestClient.getPrisonPayBands(MOORLAND_PRISON_CODE)!!

    assertThat(defaultPayBands).hasSize(3)
    assertThat(defaultPayBands.count { it.prisonCode == MOORLAND_PRISON_CODE }).isEqualTo(3)
  }

  private fun WebTestClient.getPrisonPayBands(prisonCode: String) =
    get()
      .uri("/prison/$prisonCode/prison-pay-bands")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(PrisonPayBand::class.java)
      .returnResult().responseBody
}

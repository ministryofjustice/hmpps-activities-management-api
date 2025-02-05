package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonPayBandCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonPayBandUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
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

  @Test
  fun `create a new pay band for Moorland`() {
    val request = PrisonPayBandCreateRequest(
      displaySequence = 4,
      nomisPayBand = 4,
      alias = "testAlias",
      description = "testDesc",
    )

    val payBand = webTestClient.createPayBand(MOORLAND_PRISON_CODE, request)!!

    payBand.displaySequence isEqualTo 4
    payBand.alias isEqualTo "testAlias"
    payBand.description isEqualTo "testDesc"
    payBand.nomisPayBand isEqualTo 4
    payBand.prisonCode isEqualTo "MDI"
  }

  @Test
  fun `update a created pay band for Moorland`() {
    val request = PrisonPayBandCreateRequest(
      displaySequence = 4,
      nomisPayBand = 4,
      alias = "testAlias",
      description = "testDesc",
    )

    val payBand = webTestClient.createPayBand(MOORLAND_PRISON_CODE, request)!!

    val updateRequest = PrisonPayBandUpdateRequest(
      displaySequence = 5,
      nomisPayBand = 5,
      alias = "testAlias2",
      description = "testDesc2",
    )

    val updatedPayBand = webTestClient.updatePayBand(MOORLAND_PRISON_CODE, payBand.id, updateRequest)!!

    updatedPayBand.id isEqualTo payBand.id
    updatedPayBand.displaySequence isEqualTo 5
    updatedPayBand.alias isEqualTo "testAlias2"
    updatedPayBand.description isEqualTo "testDesc2"
    updatedPayBand.nomisPayBand isEqualTo 5
    updatedPayBand.prisonCode isEqualTo "MDI"
  }

  private fun WebTestClient.getPrisonPayBands(prisonCode: String) = get()
    .uri("/prison/$prisonCode/prison-pay-bands")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(PrisonPayBand::class.java)
    .returnResult().responseBody

  private fun WebTestClient.createPayBand(prisonCode: String, request: PrisonPayBandCreateRequest): PrisonPayBand? = post()
    .uri("/prison/$prisonCode/prison-pay-band")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .bodyValue(request)
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PrisonPayBand::class.java)
    .returnResult().responseBody

  private fun WebTestClient.updatePayBand(prisonCode: String, prisonPayBandId: Long, request: PrisonPayBandUpdateRequest): PrisonPayBand? = patch()
    .uri("/prison/$prisonCode/prison-pay-band/$prisonPayBandId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .bodyValue(request)
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PrisonPayBand::class.java)
    .returnResult().responseBody
}

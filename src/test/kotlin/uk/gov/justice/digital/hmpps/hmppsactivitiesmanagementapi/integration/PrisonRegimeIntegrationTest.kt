package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.time.LocalTime

class PrisonRegimeIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-seed-rsi-prison-regime.sql",
  )
  @Test
  fun `RSI regime reflects single regime`() {
    // this test will confirm new model still works for existing prisons and code
    val regime = getPrisonRegime(agencyId = "RSI")

    assertThat(regime.amStart).isEqualTo(LocalTime.of(8, 30, 0))
    assertThat(regime.amFinish).isEqualTo(LocalTime.of(11, 45, 0))
    assertThat(regime.pmStart).isEqualTo(LocalTime.of(13, 45, 0))
    assertThat(regime.pmFinish).isEqualTo(LocalTime.of(16, 45, 0))
    assertThat(regime.edStart).isEqualTo(LocalTime.of(17, 30, 0))
    assertThat(regime.edFinish).isEqualTo(LocalTime.of(19, 15, 0))
  }

  @Disabled
  @Sql(
    "classpath:test_data/seed-seed-iwi-prison-regime.sql",
  )
  @Test
  fun `get prison regime for IWI with a monday to thursday regime, friday regime and weekend regime`() {
  }

  private fun getPrisonRegime(agencyId: String): PrisonRegime = webTestClient.get()
    .uri { builder ->
      builder
        .path("/prison/prison-regime/$agencyId")
        .build(agencyId)
    }
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, agencyId)
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(PrisonRegime::class.java)
    .returnResult().responseBody!!
}

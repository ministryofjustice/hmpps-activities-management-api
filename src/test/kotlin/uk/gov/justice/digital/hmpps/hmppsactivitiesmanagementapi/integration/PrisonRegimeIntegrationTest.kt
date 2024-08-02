package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.time.DayOfWeek
import java.time.LocalTime

class PrisonRegimeIntegrationTest : IntegrationTestBase() {

  @Test
  fun `RSI regime reflects single regime`() {
    val regime = getPrisonRegime(agencyId = "RSI")

    assertThat(regime.size).isEqualTo(1)
    assertThat(regime.first().amStart).isEqualTo(LocalTime.of(9, 0, 0))
    assertThat(regime.first().amFinish).isEqualTo(LocalTime.of(12, 0, 0))
    assertThat(regime.first().pmStart).isEqualTo(LocalTime.of(13, 45, 0))
    assertThat(regime.first().pmFinish).isEqualTo(LocalTime.of(16, 45, 0))
    assertThat(regime.first().edStart).isEqualTo(LocalTime.of(18, 0, 0))
    assertThat(regime.first().edFinish).isEqualTo(LocalTime.of(20, 0, 0))
    assertThat(regime.first().daysOfWeek.containsAll(DayOfWeek.entries)).isTrue()
  }

  @Sql(
    "classpath:test_data/seed-iwi-prison-regime.sql",
  )
  @Test
  fun `get prison regime for IWI with a monday to thursday regime, friday regime and weekend regime`() {
    val regime = getPrisonRegime(agencyId = "IWI")
    val mondayToThursdayRegime = regime.firstOrNull {
      it.daysOfWeek.containsAll(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY))
    }

    val fridayRegime = regime.firstOrNull {
      it.daysOfWeek.containsAll(listOf(DayOfWeek.FRIDAY))
    }

    val weekendRegime = regime.firstOrNull {
      it.daysOfWeek.containsAll(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
    }

    assertThat(regime.size).isEqualTo(3)

    assertThat(mondayToThursdayRegime).isNotNull
    assertThat(fridayRegime).isNotNull
    assertThat(weekendRegime).isNotNull
  }

  private fun getPrisonRegime(agencyId: String): List<PrisonRegime> = webTestClient.get()
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
    .expectBodyList(PrisonRegime::class.java)
    .returnResult().responseBody!!
}

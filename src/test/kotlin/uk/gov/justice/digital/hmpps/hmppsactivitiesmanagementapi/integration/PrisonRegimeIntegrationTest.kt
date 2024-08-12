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

    assertThat(regime.size).isEqualTo(7)
    assertThat(regime.all { it.amStart == LocalTime.of(9, 0, 0) }).isTrue()
    assertThat(regime.all { it.amFinish == LocalTime.of(12, 0, 0) }).isTrue()
    assertThat(regime.all { it.pmStart == LocalTime.of(13, 45, 0) }).isTrue()
    assertThat(regime.all { it.pmFinish == LocalTime.of(16, 45, 0) }).isTrue()
    assertThat(regime.all { it.edStart == LocalTime.of(18, 0, 0) }).isTrue()
    assertThat(regime.all { it.edFinish == LocalTime.of(20, 0, 0) }).isTrue()
    assertThat(regime.map { it.dayOfWeek }).containsAll(DayOfWeek.entries)
  }

  @Sql(
    "classpath:test_data/seed-iwi-prison-regime.sql",
  )
  @Test
  fun `get prison regime for IWI with a monday to thursday regime, friday regime and weekend regime`() {
    val regime = getPrisonRegime(agencyId = "IWI")

    assertThat(regime.first().dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
    assertThat(regime.last().dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)

    val mondayToThursdayRegime = regime.filter {
      listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY).contains(it.dayOfWeek)
    }

    val fridayRegime = regime.firstOrNull {
      it.dayOfWeek == DayOfWeek.FRIDAY
    }

    val weekendRegime = regime.filter {
      listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(it.dayOfWeek)
    }

    assertThat(regime.size).isEqualTo(7)

    assertThat(mondayToThursdayRegime).isNotEmpty
    assertThat(fridayRegime).isNotNull
    assertThat(weekendRegime).isNotEmpty

    assertThat(mondayToThursdayRegime.all { it.amStart == LocalTime.of(9, 25, 0) })
    assertThat(mondayToThursdayRegime.all { it.amFinish == LocalTime.of(11, 35, 0) })
    assertThat(mondayToThursdayRegime.all { it.pmStart == LocalTime.of(13, 40, 0) })
    assertThat(mondayToThursdayRegime.all { it.pmFinish == LocalTime.of(16, 50, 0) })
    assertThat(mondayToThursdayRegime.all { it.edStart == LocalTime.of(18, 0, 0) })
    assertThat(mondayToThursdayRegime.all { it.edFinish == LocalTime.of(19, 0, 0) })

    assertThat(weekendRegime.all { it.amStart == LocalTime.of(9, 0, 0) })
    assertThat(weekendRegime.all { it.amFinish == LocalTime.of(11, 30, 0) })
    assertThat(weekendRegime.all { it.pmStart == LocalTime.of(13, 40, 0) })
    assertThat(weekendRegime.all { it.pmFinish == LocalTime.of(16, 45, 0) })
    assertThat(weekendRegime.all { it.edStart == LocalTime.of(18, 0, 0) })
    assertThat(weekendRegime.all { it.edFinish == LocalTime.of(19, 0, 0) })

    assertThat(fridayRegime!!.amStart).isEqualTo(LocalTime.of(8, 25, 0))
    assertThat(fridayRegime.amFinish).isEqualTo(LocalTime.of(11, 35, 0))
    assertThat(fridayRegime.pmStart).isEqualTo(LocalTime.of(13, 40, 0))
    assertThat(fridayRegime.pmFinish).isEqualTo(LocalTime.of(16, 50, 0))
    assertThat(fridayRegime.edStart).isEqualTo(LocalTime.of(18, 0, 0))
    assertThat(fridayRegime.edFinish).isEqualTo(LocalTime.of(19, 0, 0))
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

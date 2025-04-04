package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.PrisonRegimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import java.time.DayOfWeek
import java.time.LocalTime

class RolloutIntegrationTest : IntegrationTestBase() {

  @Test
  fun `get active rollout prison HMP Moorland - both active activities and appointments and the prison is live`() {
    with(webTestClient.getPrisonByCode("MDI")!!) {
      assertThat(activitiesRolledOut).isTrue
      assertThat(appointmentsRolledOut).isTrue
      assertThat(prisonLive).isTrue
    }
  }

  @Test
  fun `get active rollout prison - both active activities and appointments and the prison not live`() {
    with(webTestClient.getPrisonByCode("IWI")!!) {
      assertThat(activitiesRolledOut).isTrue
      assertThat(appointmentsRolledOut).isTrue
      assertThat(prisonLive).isFalse
    }
  }

  @Test
  fun `get prisons that are rolled out for users`() {
    var prisonPlanList = webTestClient.getRolledOutPrisons()!!

    assertThat(prisonPlanList).hasSize(5)
    with(prisonPlanList) {
      this.single { it.prisonCode == "RSI" && it.prisonLive }
      this.single { it.prisonCode == "PVI" && it.prisonLive }
      this.single { it.prisonCode == "MDI" && it.prisonLive }
      this.single { it.prisonCode == "IWI" && !it.prisonLive }
      this.single { it.prisonCode == "FMI" && !it.prisonLive }
    }
  }

  @Test
  fun `get prisons that are rolled out and live for users`() {
    var prisonPlanList = webTestClient.getLiveRolledOutPrisons()!!

    assertThat(prisonPlanList).hasSize(3)
    with(prisonPlanList) {
      this.single { it.prisonCode == "RSI" && it.prisonLive }
      this.single { it.prisonCode == "PVI" && it.prisonLive }
      this.single { it.prisonCode == "MDI" && it.prisonLive }
    }
  }

  @Test
  fun `create a regime, then overwrite it`() {
    val created = webTestClient.createRegime(
      slots = DayOfWeek.entries.map {
        createRegimeSlot(dayOfWeek = it, amStart = LocalTime.of(8, 30))
      },
    )

    assertThat(created.size).isEqualTo(7)
    assertThat(created.first { it.dayOfWeek == DayOfWeek.MONDAY }.amStart).isEqualTo(LocalTime.of(8, 30))

    val updated = webTestClient.createRegime(
      slots = DayOfWeek.entries.map {
        createRegimeSlot(dayOfWeek = it, amStart = LocalTime.of(9, 30))
      },
    )

    assertThat(updated.size).isEqualTo(7)
    assertThat(updated.first { it.dayOfWeek == DayOfWeek.MONDAY }.amStart).isEqualTo(LocalTime.of(9, 30))
  }

  private fun createRegimeSlot(dayOfWeek: DayOfWeek, amStart: LocalTime): PrisonRegimeSlot = PrisonRegimeSlot(
    dayOfWeek = dayOfWeek,
    amStart = amStart,
    amFinish = LocalTime.now(),
    pmStart = LocalTime.now(),
    pmFinish = LocalTime.now(),
    edStart = LocalTime.now(),
    edFinish = LocalTime.now(),
  )

  private fun WebTestClient.createRegime(slots: List<PrisonRegimeSlot>): List<PrisonRegime> = this.post()
    .uri("/rollout/prison-regime/TST")
    .bodyValue(slots)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_MIGRATE_ACTIVITIES")))
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(PrisonRegime::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.getPrisonByCode(code: String) = get()
    .uri("/rollout/$code")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(RolloutPrisonPlan::class.java)
    .returnResult().responseBody

  private fun WebTestClient.getRolledOutPrisons() = get()
    .uri("/rollout")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(RolloutPrisonPlan::class.java)
    .returnResult().responseBody

  private fun WebTestClient.getLiveRolledOutPrisons() = get()
    .uri("/rollout?prisonsLive=true")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(RolloutPrisonPlan::class.java)
    .returnResult().responseBody
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrison
import java.time.LocalDate

class PrisonIntegrationTest : IntegrationTestBase() {

  @Test
  fun `get active rollout prison HMP Pentonville`() {
    with(webTestClient.getPrisonByCode("PVI")!!) {
      assertThat(id).isEqualTo(1)
      assertThat(code).isEqualTo("PVI")
      assertThat(description).isEqualTo("HMP Pentonville")
      assertThat(active).isTrue
    }
  }

  @Test
  fun `get inactive rollout prison HMP Moorland`() {
    with(webTestClient.getPrisonByCode("MDI")!!) {
      assertThat(id).isEqualTo(2)
      assertThat(code).isEqualTo("MDI")
      assertThat(description).isEqualTo("HMP Moorland")
      assertThat(active).isFalse
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get all scheduled prison locations for HMP Pentonville on Oct 10th 2022`() {
    val locations = webTestClient.getLocationsPrisonByCode("PVI", LocalDate.of(2022, 10, 10))

    assertThat(locations).containsExactlyInAnyOrder(
      InternalLocation(1, "L1", "Location 1"),
      InternalLocation(2, "L2", "Location 2"),
    )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get all scheduled prison locations for HMP Pentonville morning of Oct 10th 2022`() {
    val locations = webTestClient.getLocationsPrisonByCode("PVI", LocalDate.of(2022, 10, 10), TimeSlot.AM)

    assertThat(locations).containsExactly(InternalLocation(1, "L1", "Location 1"))
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get all scheduled prison locations for HMP Pentonville afternoon of Oct 10th 2022`() {
    val locations = webTestClient.getLocationsPrisonByCode("PVI", LocalDate.of(2022, 10, 10), TimeSlot.PM)

    assertThat(locations).containsExactly(InternalLocation(2, "L2", "Location 2"))
  }

  private fun WebTestClient.getPrisonByCode(code: String) =
    get()
      .uri("/prisons/$code")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(RolloutPrison::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getLocationsPrisonByCode(code: String, date: LocalDate? = LocalDate.now(), timeSlot: TimeSlot? = null) =
    get()
      .uri("/prisons/$code/locations?date=$date${timeSlot?.let { "&timeSlot=$it" } ?: ""}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(InternalLocation::class.java)
      .returnResult().responseBody
}

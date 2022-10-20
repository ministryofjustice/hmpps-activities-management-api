package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance

class AttendanceIntegrationTest : IntegrationTestBase() {

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql"
  )
  @Test
  fun `get morning attendances for a scheduled activity instance`() {
    val attendances = webTestClient.getAttendancesForInstance(1)!!

    assertThat(attendances.prisonerAttendance("A11111A").posted).isFalse
    assertThat(attendances.prisonerAttendance("A22222A").posted).isFalse
  }

  private fun WebTestClient.getAttendancesForInstance(instanceId: Long) =
    get()
      .uri("/scheduled-instances/$instanceId/attendances")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Attendance::class.java)
      .returnResult().responseBody

  private fun List<Attendance>.prisonerAttendance(prisonNumber: String) =
    firstOrNull { it.prisonerNumber.uppercase() == prisonNumber.uppercase() }
      ?: throw AssertionError("Prison attendance $prisonNumber not found.")
}

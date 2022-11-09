package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import java.time.LocalDate

class ScheduledEventIntegrationTest : IntegrationTestBase() {

  @Test
  fun `getScheduledEventsByDateRange - returns all 10 rows that satisfy the criteria`() {

    val prisonerNumber = "G4793VF"
    val bookingId = 1200993L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber)
    prisonApiMockServer.stubGetScheduledAppointments(bookingId, dateRange.start, dateRange.endInclusive)

    val scheduledEvents =
      webTestClient.getScheduledEventsByDateRange(
        "MDI",
        "G4793VF",
        dateRange.start,
        dateRange.endInclusive
      )
    Assertions.assertThat(scheduledEvents).hasSize(1)
  }

  private fun WebTestClient.getScheduledEventsByDateRange(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate
  ) =
    get()
      .uri("/prisons/$prisonCode/scheduled-events?prisonerNumber=$prisonerNumber&startDate=$startDate&endDate=$endDate")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ScheduledEvent::class.java)
      .returnResult().responseBody
}

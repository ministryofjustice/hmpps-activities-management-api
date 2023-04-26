package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentOccurrenceSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.AppointmentInstanceInformation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class AppointmentOccurrenceSearchIntegrationTest : IntegrationTestBase() {
  @Test
  fun `search appointment occurrences authorisation required`() {
    webTestClient.post()
      .uri("/appointment-occurrences/TPR/search")
      .bodyValue(AppointmentOccurrenceSearchRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointment occurrences in prison with no appointments`() {
    val request = AppointmentOccurrenceSearchRequest(
      startDate = LocalDate.now().plusDays(1),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("NAP", listOf(
      appointmentLocation(123, "NAP", userDescription = "Location 123"),
      appointmentLocation(456, "NAP", userDescription = "Location 456"),
    ))

    val results = webTestClient.searchAppointmentOccurrence("NAP", request)!!

    assertThat(results).hasSize(0)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointment occurrences in other prison`() {
    val request = AppointmentOccurrenceSearchRequest(
      startDate = LocalDate.now().plusDays(1),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("OTH", listOf(
      appointmentLocation(789, "OTH", userDescription = "Location 789"),
    ))

    val results = webTestClient.searchAppointmentOccurrence("OTH", request)!!

    assertThat(results.map { it.prisonCode }.distinct().single()).isEqualTo("OTH")
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointment occurrences starting tomorrow`() {
    val request = AppointmentOccurrenceSearchRequest(
      startDate = LocalDate.now().plusDays(1),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", listOf(
      appointmentLocation(123, "TPR", userDescription = "Location 123"),
      appointmentLocation(456, "TPR", userDescription = "Location 456"),
    ))

    val results = webTestClient.searchAppointmentOccurrence("TPR", request)!!

    assertThat(results.map { it.startDate }.distinct().single()).isEqualTo(request.startDate)
  }

  private fun WebTestClient.searchAppointmentOccurrence(
    prisonCode: String,
    request: AppointmentOccurrenceSearchRequest,
  ) =
    post()
      .uri("/appointment-occurrences/$prisonCode/search")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isAccepted
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(AppointmentOccurrenceSearchResult::class.java)
      .returnResult().responseBody
}

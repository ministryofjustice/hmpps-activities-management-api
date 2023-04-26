package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentOccurrenceSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceSearchRequest
import java.time.LocalDate

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
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(1),
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
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(1),
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
  fun `search for appointment occurrences that are part of a group appointment`() {
    val request = AppointmentOccurrenceSearchRequest(
      appointmentType = AppointmentType.GROUP,
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(1),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", listOf(
      appointmentLocation(123, "TPR", userDescription = "Location 123"),
      appointmentLocation(456, "TPR", userDescription = "Location 456"),
    ))

    val results = webTestClient.searchAppointmentOccurrence("TPR", request)!!

    assertThat(results.map { it.appointmentType }.distinct().single()).isEqualTo(request.appointmentType)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointment occurrences starting today`() {
    val request = AppointmentOccurrenceSearchRequest(
      startDate = LocalDate.now(),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", listOf(
      appointmentLocation(123, "TPR", userDescription = "Location 123"),
      appointmentLocation(456, "TPR", userDescription = "Location 456"),
    ))

    val results = webTestClient.searchAppointmentOccurrence("TPR", request)!!

    assertThat(results.map { it.startDate }.distinct().single()).isEqualTo(request.startDate)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointment occurrences starting within a week`() {
    val request = AppointmentOccurrenceSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusWeeks(1),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", listOf(
      appointmentLocation(123, "TPR", userDescription = "Location 123"),
      appointmentLocation(456, "TPR", userDescription = "Location 456"),
    ))

    val results = webTestClient.searchAppointmentOccurrence("TPR", request)!!

    results.map { it.startDate }.distinct().forEach {
      assertThat(it).isBetween(request.startDate, request.endDate)
    }

    assertThat(results.filter { it.startDate == request.startDate }).isNotEmpty
    assertThat(results.filter { it.startDate == request.endDate }).isNotEmpty
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointment occurrences that are part of an appointment with category AC1`() {
    val request = AppointmentOccurrenceSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(1),
      categoryCode = "AC1"
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", listOf(
      appointmentLocation(123, "TPR", userDescription = "Location 123"),
      appointmentLocation(456, "TPR", userDescription = "Location 456"),
    ))

    val results = webTestClient.searchAppointmentOccurrence("TPR", request)!!

    assertThat(results.map { it.category.code }.distinct().single()).isEqualTo(request.categoryCode)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointment occurrences with internal location id 123`() {
    val request = AppointmentOccurrenceSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(1),
      internalLocationId = 123
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", listOf(
      appointmentLocation(123, "TPR", userDescription = "Location 123"),
      appointmentLocation(456, "TPR", userDescription = "Location 456"),
    ))

    val results = webTestClient.searchAppointmentOccurrence("TPR", request)!!

    assertThat(results.map { it.internalLocation!!.id }.distinct().single()).isEqualTo(request.internalLocationId)
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

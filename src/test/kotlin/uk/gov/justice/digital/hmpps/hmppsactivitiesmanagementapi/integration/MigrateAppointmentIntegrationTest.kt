package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.event.appointments.appointment-instance.created=true",
  ],
)
@Transactional(readOnly = true)
class MigrateAppointmentIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @Test
  fun `migrate appointment forbidden`() {
    val request = appointmentMigrateRequest(categoryCode = "AC1")

    val error = webTestClient.post()
      .uri("/migrate-appointment")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isForbidden
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(error!!) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Access denied: Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `migrate appointment success`() {
    val request = appointmentMigrateRequest(categoryCode = "AC1")

    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf(request.prisonerNumber!!),
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = request.prisonerNumber!!,
          bookingId = 1,
          prisonId = request.prisonCode!!,
        ),
      ),
    )

    val response = webTestClient.migrateAppointment(request)!!
    verifyAppointment(response)

    verifyNoInteractions(eventsPublisher)
  }

  private fun verifyAppointment(response: AppointmentSeries) {
    with(response) {
      assertThat(id).isNotNull
      assertThat(createdBy).isEqualTo("CREATE.USER")
      assertThat(createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(appointments[0].allocations[0].prisonerNumber).isEqualTo("A1234BC")
      assertThat(categoryCode).isEqualTo("AC1")
      assertThat(prisonCode).isEqualTo("TPR")
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(inCell).isFalse()
      assertThat(startDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(startTime).isEqualTo(LocalTime.of(13, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(14, 30))
      assertThat(extraInformation).isEqualTo("Appointment level comment")
    }
  }

  private fun WebTestClient.migrateAppointment(
    request: AppointmentMigrateRequest,
  ) =
    post()
      .uri("/migrate-appointment")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody
}

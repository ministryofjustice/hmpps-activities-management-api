package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class AppointmentInstanceIntegrationTest : IntegrationTestBase() {
  @Test
  fun `get appointment instance authorisation required`() {
    webTestClient.get()
      .uri("/appointment-instances/1")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `get appointment instance`() {
    val appointmentInstance = webTestClient.getAppointmentInstanceById(3)!!

    assertThat(appointmentInstance).isEqualTo(
      AppointmentInstance(
        3,
        1,
        2,
        3,
        AppointmentType.INDIVIDUAL,
        "TPR",
        "A1234BC",
        456,
        "AC1",
        "Appointment description",
        123,
        false,
        LocalDate.now().plusDays(1),
        LocalTime.of(9, 0),
        LocalTime.of(10, 30),
        "Appointment level comment",
        appointmentInstance.createdTime,
        "TEST.USER",
        null,
        null,
      ),
    )

    assertThat(appointmentInstance.createdTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
  }

  @Test
  fun `get appointment instance by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointment-instances/-1")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
  }

  private fun WebTestClient.getAppointmentInstanceById(id: Long) =
    get()
      .uri("/appointment-instances/$id")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentInstance::class.java)
      .returnResult().responseBody
}

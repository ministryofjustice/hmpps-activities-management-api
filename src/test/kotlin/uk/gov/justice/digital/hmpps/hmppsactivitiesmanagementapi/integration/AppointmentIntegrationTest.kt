package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class AppointmentIntegrationTest : IntegrationTestBase() {
  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql"
  )
  @Test
  fun `authorisation required`() {
    webTestClient.get()
      .uri("/appointments/1")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql"
  )
  @Test
  fun `get single appointment`() {
    val appointment = webTestClient.getAppointmentById(1)

    with(appointment!!) {
      assertThat(category.code).isEqualTo("AC1")
      assertThat(category.description).isEqualTo("Appointment Category 1")
      assertThat(prisonCode).isEqualTo("TPR")
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(inCell).isEqualTo(false)
      assertThat(startDate).isEqualTo(LocalDate.now())
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(comment).isEqualTo("Appointment level comment")
      assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo("TEST.USER")
    }
  }

  @Test
  fun `get appointment by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointments/-1")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-deleted-id-2.sql"
  )
  @Test
  fun `get deleted appointment returns 404 not found`() {
    webTestClient.get()
      .uri("/appointments/2")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
  }

  private fun WebTestClient.getAppointmentById(id: Long) =
    get()
      .uri("/appointments/$id")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Appointment::class.java)
      .returnResult().responseBody
}
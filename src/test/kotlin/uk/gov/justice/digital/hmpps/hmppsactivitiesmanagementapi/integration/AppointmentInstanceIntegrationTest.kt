package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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
    val appointmentInstance = webTestClient.getAppointmentInstanceById(3)

    with(appointmentInstance!!) {
      assertThat(id).isEqualTo(3)
      assertThat(appointmentId).isEqualTo(1)
      assertThat(appointmentOccurrenceId).isEqualTo(2)
      assertThat(appointmentOccurrenceAllocationId).isEqualTo(3)
      assertThat(categoryCode).isEqualTo("AC1")
      assertThat(prisonCode).isEqualTo("TPR")
      assertThat(internalLocationId).isEqualTo(123)
      assertThat(inCell).isEqualTo(false)
      assertThat(prisonerNumber).isEqualTo("A1234BC")
      assertThat(bookingId).isEqualTo(456)
      assertThat(appointmentDate).isEqualTo(LocalDate.now())
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(comment).isEqualTo("Appointment occurrence level comment")
      assertThat(created).isCloseTo(LocalDateTime.now(), within(60, java.time.temporal.ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo("TEST.USER")
      assertThat(updated).isNull()
      assertThat(updatedBy).isNull()
    }
  }

  @Test
  fun `get appointment instance by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointment-instances/-1")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
  }

  private fun WebTestClient.getAppointmentInstanceById(id: Long) =
    get()
      .uri("/appointment-instances/$id")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentInstance::class.java)
      .returnResult().responseBody
}

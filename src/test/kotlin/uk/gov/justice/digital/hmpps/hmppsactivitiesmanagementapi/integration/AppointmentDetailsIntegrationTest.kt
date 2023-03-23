package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class AppointmentDetailsIntegrationTest : IntegrationTestBase() {
  @Test
  fun `get appointment details authorisation required`() {
    webTestClient.get()
      .uri("/appointment-details/1")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get appointment details by unknown id returns 404 not found`() {
    webTestClient.get()
      .uri("/appointments-details/-1")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-deleted-id-2.sql",
  )
  @Test
  fun `get deleted appointment details returns 404 not found`() {
    webTestClient.get()
      .uri("/appointments-details/2")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql(
    "classpath:test_data/seed-appointment-single-id-1.sql",
  )
  @Test
  fun `get single appointment details`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments("TPR", 123)
    prisonApiMockServer.stubGetUserDetailsList(listOf("TEST.USER"))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A1234BC"),
      listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC", bookingId = 456, prisonId = "TPR")),
    )

    val appointmentDetails = webTestClient.getAppointmentDetailsById(1)

    with(appointmentDetails!!) {
      assertThat(id).isEqualTo(1)
      assertThat(category).isEqualTo(AppointmentCategorySummary("AC1", "Appointment Category 1"))
      assertThat(prisonCode).isEqualTo("TPR")
      assertThat(internalLocation).isEqualTo(AppointmentLocationSummary(123, "TPR", "Test Appointment Location"))
      assertThat(inCell).isEqualTo(false)
      assertThat(startDate).isEqualTo(LocalDate.now())
      assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
      assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
      assertThat(comment).isEqualTo("Appointment level comment")
      assertThat(created).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo(UserSummary(1, "TEST.USER", "TEST1", "USER1"))
      assertThat(updated).isNull()
      assertThat(updatedBy).isNull()
      with(occurrences) {
        assertThat(size).isEqualTo(1)
        with(get(0)) {
          assertThat(id).isEqualTo(1)
          assertThat(sequenceNumber).isEqualTo(1)
          assertThat(internalLocation).isEqualTo(AppointmentLocationSummary(123, "TPR", "Test Appointment Location"))
          assertThat(inCell).isEqualTo(false)
          assertThat(startDate).isEqualTo(LocalDate.now())
          assertThat(startTime).isEqualTo(LocalTime.of(9, 0))
          assertThat(endTime).isEqualTo(LocalTime.of(10, 30))
          assertThat(comment).isEqualTo("Appointment occurrence level comment")
          assertThat(isEdited).isEqualTo(false)
          assertThat(isCancelled).isEqualTo(false)
          assertThat(updated).isNull()
          assertThat(updatedBy).isNull()
        }
      }
      with(prisoners) {
        assertThat(size).isEqualTo(1)
        with(get(0)) {
          assertThat(prisonerNumber).isEqualTo("A1234BC")
          assertThat(bookingId).isEqualTo(456)
          assertThat(firstName).isEqualTo("Tim")
          assertThat(lastName).isEqualTo("Harrison")
          assertThat(prisonCode).isEqualTo("TPR")
          assertThat(cellLocation).isEqualTo("1-2-3")
        }
      }
    }
  }

  private fun WebTestClient.getAppointmentDetailsById(id: Long) =
    get()
      .uri("/appointment-details/$id")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentDetails::class.java)
      .returnResult().responseBody
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSetDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON

abstract class AppointmentsIntegrationTestBase : IntegrationTestBase() {

  fun WebTestClient.getAppointmentDetailsById(id: Long) =
    get()
      .uri("/appointments/$id/details")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentDetails::class.java)
      .returnResult().responseBody

  fun WebTestClient.getAppointmentSetDetailsById(id: Long) =
    get()
      .uri("/appointment-set/$id/details")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSetDetails::class.java)
      .returnResult().responseBody

  fun WebTestClient.manageAppointmentAttendees(daysAfterNow: Long) {
    post()
      .uri("/job/appointments/manage-attendees?daysAfterNow=$daysAfterNow")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isAccepted
    Thread.sleep(3000)
  }

  fun WebTestClient.getAppointmentSeriesById(id: Long) =
    get()
      .uri("/appointment-series/$id")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AppointmentSeries::class.java)
      .returnResult().responseBody
}

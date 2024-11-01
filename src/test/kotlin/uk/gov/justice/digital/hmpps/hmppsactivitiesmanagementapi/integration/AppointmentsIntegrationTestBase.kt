package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentDetails
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
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.attendedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.autoSuspendedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.cancelledReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.clashReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.notRequiredReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.otherReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.refusedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.restReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.sickReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.suspendedReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON

class AttendanceReasonIntegrationTest : IntegrationTestBase() {

  @Test
  fun `get list of attendance reasons`() {
    assertThat(webTestClient.getAttendanceReasons()!!).containsExactlyInAnyOrder(
      sickReason,
      refusedReason,
      notRequiredReason,
      restReason,
      clashReason,
      otherReason,
      suspendedReason,
      autoSuspendedReason,
      cancelledReason,
      attendedReason,
    )
  }

  private fun WebTestClient.getAttendanceReasons() =
    get()
      .uri("/attendance-reasons")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(AttendanceReason::class.java)
      .returnResult().responseBody
}

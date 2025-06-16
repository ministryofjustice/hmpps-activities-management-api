package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AdvanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AdvanceAttendanceCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AdvanceAttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import java.time.temporal.ChronoUnit

class AdvanceAttendanceIntegrationTest : ActivitiesIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber("G4793VF")
  }

  @Test
  @Sql("classpath:test_data/seed-advance-attendance.sql")
  fun `create - paid is successful`() {
    val request = AdvanceAttendanceCreateRequest(
      scheduleInstanceId = 1,
      prisonerNumber = "G4793VF",
      issuePayment = true,
    )

    val attendance = webTestClient.createAdvanceAttendance(request)

    with(attendance) {
      assertThat(id).isNotNull
      assertThat(scheduleInstanceId).isEqualTo(1)
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(issuePayment).isTrue
      assertThat(payAmount).isEqualTo(125)
      assertThat(recordedTime).isCloseTo(TimeSource.now(), within(5, ChronoUnit.SECONDS))
      assertThat(recordedBy).isEqualTo("test-client")
      assertThat(attendanceHistory).isEmpty()
    }
  }

  @Test
  @Sql("classpath:test_data/seed-advance-attendance.sql")
  fun `create - unpaid is successful`() {
    val request = AdvanceAttendanceCreateRequest(
      scheduleInstanceId = 2,
      prisonerNumber = "G4793VF",
      issuePayment = false,
    )

    val attendance = webTestClient.createAdvanceAttendance(request)

    with(attendance) {
      assertThat(id).isNotNull
      assertThat(scheduleInstanceId).isEqualTo(2)
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(issuePayment).isFalse
      assertThat(payAmount).isNull()
      assertThat(recordedTime).isCloseTo(TimeSource.now(), within(5, ChronoUnit.SECONDS))
      assertThat(recordedBy).isEqualTo("test-client")
      assertThat(attendanceHistory).isEmpty()
    }
  }

  @Test
  @Sql("classpath:test_data/seed-advance-attendance.sql")
  fun `update - paid is successful`() {
    val request = AdvanceAttendanceCreateRequest(
      scheduleInstanceId = 1,
      prisonerNumber = "G4793VF",
      issuePayment = true,
    )

    val attendance = webTestClient.createAdvanceAttendance(request)

    val updatedAttendance = webTestClient.updateAdvanceAttendance(attendance!!.id, AdvanceAttendanceUpdateRequest(issuePayment = false))

    with(updatedAttendance) {
      assertThat(id).isNotNull
      assertThat(scheduleInstanceId).isEqualTo(1)
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(issuePayment).isFalse
      assertThat(payAmount).isNull()
      assertThat(recordedTime).isCloseTo(TimeSource.now(), within(5, ChronoUnit.SECONDS))
      assertThat(recordedBy).isEqualTo("test-client")
      assertThat(attendanceHistory).hasSize(1)
      with(attendanceHistory!!.first()) {
        assertThat(id).isNotNull
        assertThat(issuePayment).isTrue
        assertThat(recordedTime).isCloseTo(TimeSource.now(), within(5, ChronoUnit.SECONDS))
        assertThat(recordedBy).isEqualTo("test-client")
      }
    }
  }

  @Test
  @Sql("classpath:test_data/seed-advance-attendance.sql")
  fun `retrieve - is successful`() {
    val request = AdvanceAttendanceCreateRequest(
      scheduleInstanceId = 1,
      prisonerNumber = "G4793VF",
      issuePayment = true,
    )

    val attendance = webTestClient.createAdvanceAttendance(request)

    webTestClient.updateAdvanceAttendance(attendance!!.id, AdvanceAttendanceUpdateRequest(issuePayment = false))

    val retrievedAttendance = webTestClient.retrieveAdvanceAttendance(attendance.id)

    with(retrievedAttendance) {
      assertThat(id).isNotNull
      assertThat(scheduleInstanceId).isEqualTo(1)
      assertThat(prisonerNumber).isEqualTo("G4793VF")
      assertThat(issuePayment).isFalse
      assertThat(payAmount).isNull()
      assertThat(recordedTime).isCloseTo(TimeSource.now(), within(5, ChronoUnit.SECONDS))
      assertThat(recordedBy).isEqualTo("test-client")
      assertThat(attendanceHistory).hasSize(1)
      with(attendanceHistory!!.first()) {
        assertThat(id).isNotNull
        assertThat(issuePayment).isTrue
        assertThat(recordedTime).isCloseTo(TimeSource.now(), within(5, ChronoUnit.SECONDS))
        assertThat(recordedBy).isEqualTo("test-client")
      }
    }
  }

  @Test
  @Sql("classpath:test_data/seed-advance-attendance.sql")
  fun `delete - is successful`() {
    val request = AdvanceAttendanceCreateRequest(
      scheduleInstanceId = 1,
      prisonerNumber = "G4793VF",
      issuePayment = true,
    )

    val attendance = webTestClient.createAdvanceAttendance(request)

    webTestClient.deleteAdvanceAttendance(attendance.id)

    webTestClient.get()
      .uri("/advance-attendances/${attendance.id}")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .header(CASELOAD_ID, "PVI")
      .exchange()
      .expectStatus().isNotFound
  }

  private fun WebTestClient.updateAdvanceAttendance(id: Long, request: AdvanceAttendanceUpdateRequest) = put()
    .uri("/advance-attendances/$id")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .header(CASELOAD_ID, "PVI")
    .exchange()
    .expectStatus().isAccepted
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AdvanceAttendance::class.java)
    .returnResult().responseBody

  private fun WebTestClient.deleteAdvanceAttendance(id: Long) = delete()
    .uri("/advance-attendances/$id")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .header(CASELOAD_ID, "PVI")
    .exchange()
    .expectStatus().isOk
}

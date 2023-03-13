package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance as EntityAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance

class AttendanceIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var attendanceRepository: AttendanceRepository

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get morning attendances for a scheduled activity instance`() {
    val attendances = webTestClient.getAttendancesForInstance(1)!!

    assertThat(attendances.prisonerAttendanceReason("A11111A").attendanceReason).isNull()
    assertThat(attendances.prisonerAttendanceReason("A22222A").attendanceReason).isNull()
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `morning attendances are marked for an activity with attendance records`() {
    val unmarkedAttendances = attendanceRepository.findAll().also { assertThat(it).hasSize(2) }
    unmarkedAttendances.forEach { assertThat(it.attendanceReason).isNull() }

    webTestClient
      .put()
      .uri("/attendances")
      .bodyValue(
        listOf(
          AttendanceUpdateRequest(1, "ATT"),
          AttendanceUpdateRequest(2, "SICK"),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_ACTIVITY_ADMIN")))
      .exchange()
      .expectStatus().isNoContent

    val markedAttendances = attendanceRepository.findAll().toList().also { assertThat(it).hasSize(2) }
    assertThat(markedAttendances.prisonerAttendanceReason("A11111A").code).isEqualTo("ATT")
    assertThat(markedAttendances.prisonerAttendanceReason("A22222A").code).isEqualTo("SICK")
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `403 error morning attendances are marked without the correct role`() {
    val unmarkedAttendances = attendanceRepository.findAll().also { assertThat(it).hasSize(2) }
    unmarkedAttendances.forEach { assertThat(it.attendanceReason).isNull() }

    val error = webTestClient.put()
      .uri("/attendances")
      .bodyValue(
        listOf(
          AttendanceUpdateRequest(1, "ATT", null, null, null, null),
          AttendanceUpdateRequest(2, "ABS", null, null, null, null),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_NOT_ALLOWED")))
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

  private fun WebTestClient.getAttendancesForInstance(instanceId: Long) =
    get()
      .uri("/scheduled-instances/$instanceId/attendances")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ModelAttendance::class.java)
      .returnResult().responseBody

  private fun List<EntityAttendance>.prisonerAttendanceReason(prisonNumber: String) =
    firstOrNull { it.prisonerNumber.uppercase() == prisonNumber.uppercase() }.let { it?.attendanceReason }
      ?: throw AssertionError("Prison attendance $prisonNumber not found.")

  private fun List<ModelAttendance>.prisonerAttendanceReason(prisonNumber: String) =
    firstOrNull { it.prisonerNumber.uppercase() == prisonNumber.uppercase() }
      ?: throw AssertionError("Prison attendance $prisonNumber not found.")
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerAttendanceInformation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance as EntityAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendance as ModelAllAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance

@TestPropertySource(
  properties = [
    "feature.events.sns.enabled=true",
    "feature.event.activities.prisoner.attendance-created=true",
    "feature.event.activities.prisoner.attendance-amended=true",
  ],
)
class AttendanceIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  @Autowired
  private lateinit var attendanceRepository: AttendanceRepository

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

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
    "classpath:test_data/seed-activity-for-attendance-marking.sql",
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
          AttendanceUpdateRequest(1, PENTONVILLE_PRISON_CODE, AttendanceStatus.COMPLETED, "ATTENDED", null, null, null, null, null),
          AttendanceUpdateRequest(2, PENTONVILLE_PRISON_CODE, AttendanceStatus.COMPLETED, "SICK", null, null, null, null, null),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().isNoContent

    val markedAttendances = attendanceRepository.findAll().toList().also { assertThat(it).hasSize(2) }
    assertThat(markedAttendances.prisonerAttendanceReason("A11111A").code).isEqualTo(AttendanceReasonEnum.ATTENDED)
    assertThat(markedAttendances.prisonerAttendanceReason("A22222A").code).isEqualTo(AttendanceReasonEnum.SICK)

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    // Should detect the 2x attendance update events
    eventCaptor.allValues.map {
      assertThat(it.eventType).isEqualTo("activities.prisoner.attendance-amended")
      assertThat(it.additionalInformation).isIn(
        listOf(
          PrisonerAttendanceInformation(1),
          PrisonerAttendanceInformation(2),
        ),
      )
      assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(it.description).isEqualTo("A prisoner attendance has been amended in the activities management service")
    }
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
          AttendanceUpdateRequest(1, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "ATTENDED", null, null, null, null, null),
          AttendanceUpdateRequest(2, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "SICK", null, null, null, null, null),
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

    // Check that no sync events were emitted
    verify(eventsPublisher, times(0)).send(eventCaptor.capture())
  }

  @Sql(
    "classpath:test_data/seed-activity-id-18.sql",
  )
  @Test
  fun `marked attendance is updated to produce history record`() {
    attendanceRepository.findAll().also { assertThat(it).hasSize(1) }

    webTestClient
      .put()
      .uri("/attendances")
      .bodyValue(
        listOf(
          AttendanceUpdateRequest(1, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "SICK", null, true, null, null, null),
        ),
      )
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().isNoContent

    val updatedAttendances = attendanceRepository.findAll().toList().also { assertThat(it).hasSize(1) }
    assertThat(updatedAttendances.prisonerAttendanceReason("A11111A").code).isEqualTo(AttendanceReasonEnum.SICK)
    assertThat(updatedAttendances[0].history()).hasSize(1)

    verify(eventsPublisher).send(eventCaptor.capture())

    // Should detect the attendance updated event
    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.prisoner.attendance-amended")
      assertThat(additionalInformation).isEqualTo(PrisonerAttendanceInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A prisoner attendance has been amended in the activities management service")
    }
  }

  @Sql(
    "classpath:test_data/seed-attendances.sql",
  )
  @Test
  fun `get attendance list for specified date`() {
    val attendanceList = webTestClient.getAllAttendanceByDate(MOORLAND_PRISON_CODE, LocalDate.of(2022, 10, 10))!!
    assertThat(attendanceList.size).isEqualTo(9)
  }

  private fun WebTestClient.getAttendancesForInstance(instanceId: Long) =
    get()
      .uri("/scheduled-instances/$instanceId/attendances")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ModelAttendance::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getAllAttendanceByDate(prisonCode: String, sessionDate: LocalDate) =
    get()
      .uri("/attendances/$prisonCode/$sessionDate")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ModelAllAttendance::class.java)
      .returnResult().responseBody

  private fun List<EntityAttendance>.prisonerAttendanceReason(prisonNumber: String) =
    firstOrNull { it.prisonerNumber.uppercase() == prisonNumber.uppercase() }.let { it?.attendanceReason }
      ?: throw AssertionError("Prison attendance $prisonNumber not found.")

  private fun List<ModelAttendance>.prisonerAttendanceReason(prisonNumber: String) =
    firstOrNull { it.prisonerNumber.uppercase() == prisonNumber.uppercase() }
      ?: throw AssertionError("Prison attendance $prisonNumber not found.")
}

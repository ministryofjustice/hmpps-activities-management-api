package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTierType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.RISLEY_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
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
class AttendanceIntegrationTest : ActivitiesIntegrationTestBase() {

  @Autowired
  private lateinit var attendanceRepository: AttendanceRepository

  @Autowired
  private lateinit var attendanceHistoryRepository: AttendanceHistoryRepository

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get morning attendances for a scheduled activity instance`() {
    val attendances = webTestClient.getScheduledInstancesByIds(1)!!.first().attendances

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
    assertThat(attendanceRepository.findAll()).hasSize(3)

    val error = webTestClient.put()
      .uri("/attendances")
      .bodyValue(
        listOf(
          AttendanceUpdateRequest(1, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "ATTENDED", null, null, null, null, null),
          AttendanceUpdateRequest(2, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "SICK", null, null, null, null, null),
          AttendanceUpdateRequest(3, MOORLAND_PRISON_CODE, AttendanceStatus.COMPLETED, "REFUSED", null, null, null, null, null),
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

    assertThat(attendanceRepository.findById(1).get().attendanceReason).isNull()
    assertThat(attendanceRepository.findById(2).get().attendanceReason).isNull()
    assertThat(attendanceRepository.findById(3).get().attendanceReason!!.code).isEqualTo(AttendanceReasonEnum.SICK)

    // Check that no sync events were emitted
    verify(eventsPublisher, never()).send(eventCaptor.capture())
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

    val history = attendanceHistoryRepository.findAll()
    assertThat(history.filter { it.attendance.attendanceId == updatedAttendances[0].attendanceId }).hasSize(1)

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
    assertThat(attendanceList.any { it.eventTier == EventTierType.TIER_1 }).isTrue()
    assertThat(attendanceList.filter { it.incentiveLevelWarningIssued == null }.map { it.attendanceId }).containsOnly(1, 2, 3, 4, 5, 6, 7)
    assertThat(attendanceList.filter { it.incentiveLevelWarningIssued == false }.map { it.attendanceId }).containsOnly(8)
    assertThat(attendanceList.filter { it.incentiveLevelWarningIssued == true }.map { it.attendanceId }).containsOnly(9)
  }

  @Sql(
    "classpath:test_data/seed-attendances.sql",
  )
  @Test
  fun `get attendance list for specified date and event tier`() {
    val attendanceList = webTestClient.getAllAttendanceByDate(
      prisonCode = MOORLAND_PRISON_CODE,
      sessionDate = LocalDate.of(2022, 10, 10),
      eventTierType = EventTierType.TIER_2,
    )!!

    assertThat(attendanceList.size).isEqualTo(3)
    assertThat(attendanceList.all { it.eventTier == EventTierType.TIER_2 }).isTrue()
  }

  @Sql(
    "classpath:test_data/seed-suspended-attendance.sql",
  )
  @Test
  fun `get suspended prisoner activity attendance`() {
    webTestClient.get()
      .uri("/attendances/$RISLEY_PRISON_CODE/suspended?date=${LocalDate.now()}")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$.[0].prisonerNumber").isEqualTo("A11111A")
      .jsonPath("$.[0].attendance[0].timeSlot").isEqualTo("AM")
      .jsonPath("$.[0].attendance[0].inCell").isEqualTo(false)
      .jsonPath("$.[0].attendance[0].onWing").isEqualTo(false)
      .jsonPath("$.[0].attendance[0].offWing").isEqualTo(false)
      .jsonPath("$.[0].attendance[0].categoryName").isEqualTo("Education")
      .jsonPath("$.[0].attendance[0].startTime").isEqualTo("09:00:00")
      .jsonPath("$.[0].attendance[0].endTime").isEqualTo("11:00:00")
      .jsonPath("$.[0].attendance[0].internalLocation").isEmpty
      .jsonPath("$.[0].attendance[0].attendanceReasonCode").isEqualTo("SUSPENDED")
      .jsonPath("$.[0].attendance[0].scheduledInstanceId").isEqualTo(1)
      .jsonPath("$.[0].attendance[0].activitySummary").isEqualTo("Maths")
  }

  private fun WebTestClient.getAllAttendanceByDate(prisonCode: String, sessionDate: LocalDate, eventTierType: EventTierType? = null) =
    get()
      .uri("/attendances/$prisonCode/$sessionDate${eventTierType?.let { "?eventTier=${it.name}" } ?: ""}")
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

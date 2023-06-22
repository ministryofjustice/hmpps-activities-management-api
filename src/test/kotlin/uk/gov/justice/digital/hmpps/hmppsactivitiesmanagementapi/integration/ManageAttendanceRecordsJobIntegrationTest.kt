package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.events.sns.enabled=true",
    "feature.event.activities.prisoner.attendance-created=true",
    "feature.event.activities.prisoner.attendance-amended=true",
    "feature.event.activities.prisoner.attendance-expired=true",
  ],
)
class ManageAttendanceRecordsJobIntegrationTest : IntegrationTestBase() {
  @MockBean
  private lateinit var eventsPublisher: OutboundEventsPublisher

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Autowired
  private lateinit var attendanceRepository: AttendanceRepository

  @Autowired
  private lateinit var activityRepository: ActivityRepository

  @Sql("classpath:test_data/seed-activity-id-4.sql")
  @Test
  fun `Four attendance records are created, 2 for Maths level 1 AM and 2 for Maths Level 1 PM and four create events are emitted`() {
    val allocatedPrisoners = listOf(listOf("A22222A", "A11111A"), listOf("A44444A", "A33333A"))
    allocatedPrisoners.forEach {
      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = it,
        prisoners = it.map { prisonNumber ->
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = prisonNumber, prisonId = "PVI")
        },
      )
    }

    assertThat(attendanceRepository.count()).isZero

    with(activityRepository.findById(4).orElseThrow()) {
      assertThat(description).isEqualTo("Maths Level 1")
      assertThat(schedules()).hasSize(2)

      with(schedules().findByDescription("Maths AM")) {
        assertThat(allocations()).hasSize(2)
        assertThat(instances()).hasSize(1)
        assertThat(instances().first().attendances).isEmpty()
      }

      with(schedules().findByDescription("Maths PM")) {
        assertThat(allocations()).hasSize(2)
        assertThat(instances()).hasSize(1)
        assertThat(instances().first().attendances).isEmpty()
      }
    }

    webTestClient.manageAttendanceRecords()

    with(activityRepository.findById(4).orElseThrow()) {
      with(schedules().findByDescription("Maths AM")) {
        assertThat(instances().first().attendances).hasSize(2)
      }
      with(schedules().findByDescription("Maths PM")) {
        assertThat(instances().first().attendances).hasSize(2)
      }
    }

    assertThat(attendanceRepository.count()).isEqualTo(4)

    verify(eventsPublisher, times(4)).send(eventCaptor.capture())

    eventCaptor.allValues.map {
      assertThat(it.eventType).isEqualTo("activities.prisoner.attendance-created")
      assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(it.description).isEqualTo("A prisoner attendance has been created in the activities management service")
    }
  }

  @Sql("classpath:test_data/seed-activity-id-4.sql")
  @Test
  fun `Multiple calls on same day does not result in duplicate attendances`() {
    val allocatedPrisoners = listOf(listOf("A22222A", "A11111A"), listOf("A44444A", "A33333A"))
    allocatedPrisoners.forEach {
      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = it,
        prisoners = it.map { prisonNumber ->
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = prisonNumber, prisonId = "PVI")
        },
      )
    }

    assertThat(attendanceRepository.count()).isZero

    webTestClient.manageAttendanceRecords()
    webTestClient.manageAttendanceRecords()

    assertThat(attendanceRepository.count()).isEqualTo(4)
  }

  @Sql("classpath:test_data/seed-activity-id-5.sql")
  @Test
  fun `No attendance records are created for gym induction AM when attendance not required on activity`() {
    assertThat(attendanceRepository.count()).isZero

    with(activityRepository.findById(5).orElseThrow()) {
      assertThat(description).isEqualTo("Gym induction")
      assertThat(schedules()).hasSize(1)

      with(schedules().findByDescription("Gym induction AM")) {
        assertThat(allocations()).hasSize(2)
        assertThat(instances()).hasSize(1)
        assertThat(instances().first().attendances).isEmpty()
      }
    }

    webTestClient.manageAttendanceRecords()

    with(activityRepository.findById(5).orElseThrow()) {
      with(schedules().findByDescription("Gym induction AM")) {
        assertThat(instances().first().attendances).isEmpty()
      }
    }

    assertThat(attendanceRepository.count()).isZero
  }

  @Sql("classpath:test_data/seed-attendances-yesterdays-completed.sql")
  @Test
  fun `Yesterdays completed attendance records remain in status COMPLETED and no sync events are emitted`() {
    val yesterday = LocalDate.now().minusDays(1)

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
      assertThat(it.scheduledInstance.sessionDate).isEqualTo(yesterday)
    }

    webTestClient.manageAttendanceRecordsWithExpiry()

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
    }

    verifyNoInteractions(eventsPublisher)
  }

  @Sql("classpath:test_data/seed-attendances-yesterdays-waiting.sql")
  @Test
  fun `Yesterdays waiting attendance records emit an expired sync event and remain in WAITING status`() {
    val yesterday = LocalDate.now().minusDays(1)

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(it.scheduledInstance.sessionDate).isEqualTo(yesterday)
    }

    webTestClient.manageAttendanceRecordsWithExpiry()

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    eventCaptor.allValues.map {
      assertThat(it.eventType).isEqualTo("activities.prisoner.attendance-expired")
      assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(it.description).isEqualTo("An unmarked prisoner attendance has been expired in the activities management service")
    }
  }

  @Sql("classpath:test_data/seed-attendances-two-days-old-waiting.sql")
  @Test
  fun `Two day old waiting attendance records do not emit an expired sync event`() {
    val twoDaysAgo = LocalDate.now().minusDays(2)

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(it.scheduledInstance.sessionDate).isEqualTo(twoDaysAgo)
    }

    webTestClient.manageAttendanceRecordsWithExpiry()

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
    }

    verifyNoInteractions(eventsPublisher)
  }

  @Sql("classpath:test_data/seed-attendances-for-today.sql")
  @Test
  fun `Attendance records for today will not emit an expired sync event - too early`() {
    val today = LocalDate.now()

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isIn(AttendanceStatus.WAITING, AttendanceStatus.COMPLETED)
      assertThat(it.scheduledInstance.sessionDate).isEqualTo(today)
    }

    webTestClient.manageAttendanceRecordsWithExpiry()

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isIn(AttendanceStatus.WAITING, AttendanceStatus.COMPLETED)
    }

    verifyNoInteractions(eventsPublisher)
  }

  private fun List<ActivitySchedule>.findByDescription(description: String) =
    first { it.description.uppercase() == description.uppercase() }

  private fun WebTestClient.manageAttendanceRecords() {
    post()
      .uri("/job/manage-attendance-records")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
    Thread.sleep(1000)
  }

  private fun WebTestClient.manageAttendanceRecordsWithExpiry() {
    post()
      .uri("/job/manage-attendance-records?withExpiry=true")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
    Thread.sleep(1000)
  }
}

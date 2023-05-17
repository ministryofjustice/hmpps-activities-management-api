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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.events.sns.enabled=true",
    "feature.event.activities.prisoner.attendance-created=true",
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
    assertThat(attendanceRepository.count()).isZero

    webTestClient.manageAttendanceRecords()
    webTestClient.manageAttendanceRecords()

    assertThat(attendanceRepository.count()).isEqualTo(4)
  }

  @Sql("classpath:test_data/seed-activity-id-5.sql")
  @Test
  fun `No attendance records are created for gym induction AM`() {
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
}

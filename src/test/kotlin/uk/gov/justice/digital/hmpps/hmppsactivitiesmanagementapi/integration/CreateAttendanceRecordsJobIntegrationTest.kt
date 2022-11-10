package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository

class CreateAttendanceRecordsJobIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var attendanceRepository: AttendanceRepository

  @Autowired
  private lateinit var activityRepository: ActivityRepository

  @Sql("classpath:test_data/seed-activity-id-4.sql")
  @Test
  fun `Four attendance records are created, 2 for Maths level 1 AM and 2 for Maths Level 1 PM`() {
    assertThat(attendanceRepository.count()).isZero

    with(activityRepository.findById(4).orElseThrow()) {
      assertThat(description).isEqualTo("Maths Level 1")
      assertThat(schedules).hasSize(2)

      with(schedules.findByDescription("Maths AM")) {
        assertThat(allocations).hasSize(2)
        assertThat(instances).hasSize(1)
        assertThat(instances.first().attendances).isEmpty()
      }

      with(schedules.findByDescription("Maths PM")) {
        assertThat(allocations).hasSize(2)
        assertThat(instances).hasSize(1)
        assertThat(instances.first().attendances).isEmpty()
      }
    }

    webTestClient.createAttendanceRecords()

    with(activityRepository.findById(4).orElseThrow()) {
      with(schedules.findByDescription("Maths AM")) {
        assertThat(instances.first().attendances).hasSize(2)
      }
      with(schedules.findByDescription("Maths PM")) {
        assertThat(instances.first().attendances).hasSize(2)
      }
    }

    assertThat(attendanceRepository.count()).isEqualTo(4)
  }

  @Sql("classpath:test_data/seed-activity-id-4.sql")
  @Test
  fun `Multiple calls on same day does not result in duplicate attendances`() {
    assertThat(attendanceRepository.count()).isZero

    webTestClient.createAttendanceRecords()
    webTestClient.createAttendanceRecords()

    assertThat(attendanceRepository.count()).isEqualTo(4)
  }

  private fun List<ActivitySchedule>.findByDescription(description: String) =
    first { it.description.uppercase() == description.uppercase() }

  private fun WebTestClient.createAttendanceRecords() {
    post()
      .uri("/job/create-attendance-records")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isCreated
    Thread.sleep(1000)
  }
}

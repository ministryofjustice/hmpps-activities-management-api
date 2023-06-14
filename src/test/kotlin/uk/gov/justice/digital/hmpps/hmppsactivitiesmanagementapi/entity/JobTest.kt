package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class JobTest {

  private val start = LocalDateTime.MIN

  @Test
  fun `successful job`() {
    val job = Job.successful(JobType.ATTENDANCE_CREATE, start)

    assertThat(job.jobType).isEqualTo(JobType.ATTENDANCE_CREATE)
    assertThat(job.startedAt).isEqualTo(start)
    assertThat(job.endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
    assertThat(job.successful).isTrue
  }

  @Test
  fun `failed job`() {
    val job = Job.failed(JobType.ATTENDANCE_CREATE, start)

    assertThat(job.jobType).isEqualTo(JobType.ATTENDANCE_CREATE)
    assertThat(job.startedAt).isEqualTo(start)
    assertThat(job.endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
    assertThat(job.successful).isFalse
  }

  @Test
  fun `job cannot be changed once successful or failed`() {
    with(Job.successful(JobType.ATTENDANCE_CREATE, LocalDateTime.now())) {
      assertThatThrownBy { succeeded() }.isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Job is already ended.")
    }

    with(Job.failed(JobType.ATTENDANCE_CREATE, LocalDateTime.now())) {
      assertThatThrownBy { failed() }.isInstanceOf(IllegalStateException::class.java)
        .hasMessage("Job is already ended.")
    }
  }
}

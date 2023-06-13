package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class JobTest {

  @Test
  fun `can successful job`() {
    val job = Job.start(JobType.ATTENDANCE)

    assertThat(job.jobType).isEqualTo(JobType.ATTENDANCE)
    assertThat(job.startedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
    assertThat(job.endedAt).isNull()

    job.succeeded()

    assertThat(job.endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
    assertThat(job.successful).isTrue
  }

  @Test
  fun `cannot end already ended job`() {
    val job = Job.start(JobType.ATTENDANCE).succeeded()

    assertThatThrownBy { job.succeeded() }.isInstanceOf(IllegalStateException::class.java).hasMessage("Job is already ended.")
  }
}

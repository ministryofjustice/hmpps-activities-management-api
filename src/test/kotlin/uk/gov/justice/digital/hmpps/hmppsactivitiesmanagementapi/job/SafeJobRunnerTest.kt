package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SafeJobRunnerTest {

  private val jobRepository: JobRepository = mock()
  private val runner = SafeJobRunner(jobRepository)
  private val jobEntityCaptor = argumentCaptor<Job>()

  @Test
  fun `runs safe job without error`() {
    runner.runSafe(JobDefinition(JobType.ATTENDANCE) {})

    verify(jobRepository).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      assertThat(endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(successful).isTrue
    }
  }

  @Test
  fun `runs safe multiple jobs without error`() {
    runner.runSafe(
      JobDefinition(JobType.ATTENDANCE) {},
      JobDefinition(JobType.DEALLOCATION) {},
    )

    verify(jobRepository, times(2)).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      assertThat(endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(successful).isTrue()
    }

    with(jobEntityCaptor.secondValue) {
      assertThat(endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(successful).isTrue()
    }
  }

  @Test
  fun `runs safe job with error`() {
    runner.runSafe(JobDefinition(JobType.ATTENDANCE) { throw RuntimeException("it failed") })

    verify(jobRepository).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      assertThat(endedAt).isNull()
      assertThat(successful).isFalse
    }
  }

  @Test
  fun `runs safe multiple jobs with error on first job`() {
    runner.runSafe(
      JobDefinition(JobType.ATTENDANCE) { throw RuntimeException("first job failed") },
      JobDefinition(JobType.DEALLOCATION) {},
    )

    verify(jobRepository, times(2)).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      assertThat(endedAt).isNull()
      assertThat(successful).isFalse
    }

    with(jobEntityCaptor.secondValue) {
      assertThat(endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(successful).isTrue()
    }
  }
}

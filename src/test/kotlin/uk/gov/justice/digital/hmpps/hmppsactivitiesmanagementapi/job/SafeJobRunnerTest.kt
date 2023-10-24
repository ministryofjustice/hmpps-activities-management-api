package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SafeJobRunnerTest {

  private val jobRepository: JobRepository = mock()
  private val runner = SafeJobRunner(jobRepository, TransactionHandler())
  private val jobEntityCaptor = argumentCaptor<Job>()

  @Test
  fun `runs job without error`() {
    runner.runJob(JobDefinition(JobType.ATTENDANCE_CREATE) {})

    verify(jobRepository).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      assertThat(jobType).isEqualTo(JobType.ATTENDANCE_CREATE)
      assertThat(endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(successful).isTrue
    }
  }

  @Test
  fun `runs all dependent jobs without error`() {
    runner.runDependentJobs(
      JobDefinition(JobType.ATTENDANCE_CREATE) {},
      JobDefinition(JobType.DEALLOCATE_ENDING) {},
    )

    verify(jobRepository, times(2)).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      assertThat(jobType).isEqualTo(JobType.ATTENDANCE_CREATE)
      assertThat(endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(successful).isTrue()
    }

    with(jobEntityCaptor.secondValue) {
      assertThat(jobType).isEqualTo(JobType.DEALLOCATE_ENDING)
      assertThat(endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(successful).isTrue()
    }
  }

  @Test
  fun `runs job with error`() {
    assertThatThrownBy {
      runner.runJob(JobDefinition(JobType.ATTENDANCE_CREATE) { throw RuntimeException("it failed") })
    }.isInstanceOf(RuntimeException::class.java)
      .hasMessage("Failure occurred running job ${JobType.ATTENDANCE_CREATE}")

    verify(jobRepository).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      assertThat(jobType).isEqualTo(JobType.ATTENDANCE_CREATE)
      assertThat(endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(successful).isFalse
    }
  }

  @Test
  fun `runs dependent jobs with error on first job`() {
    assertThatThrownBy {
      runner.runDependentJobs(
        JobDefinition(JobType.ATTENDANCE_CREATE) { throw RuntimeException("first job failed") },
        JobDefinition(JobType.DEALLOCATE_ENDING) {},
      )
    }.isInstanceOf(RuntimeException::class.java)
      .hasMessage("Failure occurred running job ${JobType.ATTENDANCE_CREATE}")

    verify(jobRepository, times(2)).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      assertThat(jobType).isEqualTo(JobType.ATTENDANCE_CREATE)
      assertThat(endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(successful).isFalse
    }

    with(jobEntityCaptor.secondValue) {
      assertThat(jobType).isEqualTo(JobType.DEALLOCATE_ENDING)
      assertThat(endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(successful).isFalse
    }
  }

  @Test
  fun `runs dependent jobs with error on second job`() {
    assertThatThrownBy {
      runner.runDependentJobs(
        JobDefinition(JobType.ATTENDANCE_CREATE) { },
        JobDefinition(JobType.DEALLOCATE_ENDING) { throw RuntimeException("first job failed") },
      )
    }.isInstanceOf(RuntimeException::class.java)
      .hasMessage("Failure occurred running job ${JobType.DEALLOCATE_ENDING}")

    verify(jobRepository, times(2)).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      assertThat(jobType).isEqualTo(JobType.ATTENDANCE_CREATE)
      assertThat(endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(successful).isTrue
    }

    with(jobEntityCaptor.secondValue) {
      assertThat(jobType).isEqualTo(JobType.DEALLOCATE_ENDING)
      assertThat(endedAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(successful).isFalse
    }
  }
}

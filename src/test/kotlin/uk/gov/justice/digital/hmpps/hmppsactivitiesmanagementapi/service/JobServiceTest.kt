package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import java.time.LocalDateTime
import java.util.*

class JobServiceTest {
  val jobRepository: JobRepository = mock()
  val jobService = JobService(jobRepository)

  @Test
  fun `should initialise counts`() {
    jobService.initialiseCounts(123, 2)

    verify(jobRepository).initialiseCounts(123, 2)
  }

  @Test
  fun `should increment count and not complete job`() {
    val job = Job(123, JobType.SCHEDULES, LocalDateTime.now().minusMinutes(1))
    job.completedSubTasks = 1
    job.totalSubTasks = 2

    whenever(jobRepository.findById(123)).thenReturn(Optional.of(job))

    jobService.incrementCount(123)

    verify(jobRepository).incrementCount(123)

    assertThat(job.successful).isFalse()
  }

  @Test
  fun `should increment count and complete job`() {
    val job = Job(123, JobType.SCHEDULES, LocalDateTime.now().minusMinutes(1))
    job.completedSubTasks = 2
    job.totalSubTasks = 2

    whenever(jobRepository.findById(123)).thenReturn(Optional.of(job))

    jobService.incrementCount(123)

    verify(jobRepository).incrementCount(123)

    assertThat(job.successful).isTrue()
  }
}

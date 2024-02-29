package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Retryable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MonitoringService

class SafeJobRunnerTest {

  private val jobRepository: JobRepository = mock()
  private val monitoringService: MonitoringService = mock()
  private val retryable: Retryable = mock()
  private val runner = SafeJobRunner(jobRepository, monitoringService, retryable)
  private val jobEntityCaptor = argumentCaptor<Job>()

  @Test
  fun `runs job without error`() {
    runner.runJob(JobDefinition(JobType.ATTENDANCE_CREATE) {})

    verify(jobRepository).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      jobType isEqualTo JobType.ATTENDANCE_CREATE
      endedAt isCloseTo TimeSource.now()
      successful isBool true
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
      jobType isEqualTo JobType.ATTENDANCE_CREATE
      endedAt isCloseTo TimeSource.now()
      successful isBool true
    }

    with(jobEntityCaptor.secondValue) {
      jobType isEqualTo JobType.DEALLOCATE_ENDING
      endedAt isCloseTo TimeSource.now()
      successful isBool true
    }
  }

  @Test
  fun `runs job with error`() {
    whenever(jobRepository.saveAndFlush(any())) doReturn Job(1, JobType.ATTENDANCE_CREATE, TimeSource.now())

    runner.runJob(JobDefinition(JobType.ATTENDANCE_CREATE) { throw RuntimeException("it failed") })

    verify(jobRepository).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      jobType isEqualTo JobType.ATTENDANCE_CREATE
      endedAt isCloseTo TimeSource.now()
      successful isBool false
    }

    verify(monitoringService).capture("Job 'ATTENDANCE_CREATE' for job id '1' failed")
  }

  @Test
  fun `runs dependent jobs with error on first job`() {
    whenever(jobRepository.saveAndFlush(argThat { j -> j?.jobType == JobType.ATTENDANCE_CREATE && !j.successful })) doReturn Job(1, JobType.ATTENDANCE_CREATE, TimeSource.now())
    whenever(jobRepository.saveAndFlush(argThat { j -> j?.jobType == JobType.DEALLOCATE_ENDING && !j.successful })) doReturn Job(2, JobType.DEALLOCATE_ENDING, TimeSource.now())

    runner.runDependentJobs(
      JobDefinition(JobType.ATTENDANCE_CREATE) { throw RuntimeException("first job failed") },
      JobDefinition(JobType.DEALLOCATE_ENDING) {},
    )

    verify(jobRepository, times(2)).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      jobType isEqualTo JobType.ATTENDANCE_CREATE
      endedAt isCloseTo TimeSource.now()
      successful isBool false
    }

    with(jobEntityCaptor.secondValue) {
      jobType isEqualTo JobType.DEALLOCATE_ENDING
      endedAt isCloseTo TimeSource.now()
      successful isBool false
    }

    verify(monitoringService).capture("Job 'ATTENDANCE_CREATE' for job id '1' failed")
    verify(monitoringService).capture("Dependant job 'DEALLOCATE_ENDING' for job id '2' failed")
  }

  @Test
  fun `runs dependent jobs with error on second job`() {
    whenever(jobRepository.saveAndFlush(any())) doReturn Job(3, JobType.DEALLOCATE_ENDING, TimeSource.now())

    runner.runDependentJobs(
      JobDefinition(JobType.ATTENDANCE_CREATE) { },
      JobDefinition(JobType.DEALLOCATE_ENDING) { throw RuntimeException("first job failed") },
    )

    verify(jobRepository, times(2)).saveAndFlush(jobEntityCaptor.capture())

    with(jobEntityCaptor.firstValue) {
      jobType isEqualTo JobType.ATTENDANCE_CREATE
      endedAt isCloseTo TimeSource.now()
      successful isBool true
    }

    with(jobEntityCaptor.secondValue) {
      jobType isEqualTo JobType.DEALLOCATE_ENDING
      endedAt isCloseTo TimeSource.now()
      successful isBool false
    }

    verify(monitoringService).capture("Job 'DEALLOCATE_ENDING' for job id '3' failed")
  }
}

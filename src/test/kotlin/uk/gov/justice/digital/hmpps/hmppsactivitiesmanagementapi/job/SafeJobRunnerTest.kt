package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
  private val job = Job(jobId = 1, JobType.ATTENDANCE_CREATE, TimeSource.now())
  private val runner = SafeJobRunner(jobRepository, monitoringService) { it() }

  @Test
  fun `runs job without error`() {
    whenever(jobRepository.saveAndFlush(any())) doReturn job

    runner.runJob(JobDefinition(JobType.ATTENDANCE_CREATE) {})

    with(job) {
      jobType isEqualTo JobType.ATTENDANCE_CREATE
      endedAt isCloseTo TimeSource.now()
      successful isBool true
    }
  }

  @Test
  fun `runs job succeeds on retry`() {
    whenever(jobRepository.saveAndFlush(any())) doReturn job

    runner.runJobWithRetry(
      JobDefinition(JobType.ATTENDANCE_CREATE) {},
    )

    with(job) {
      jobType isEqualTo JobType.ATTENDANCE_CREATE
      endedAt isCloseTo TimeSource.now()
      successful isBool true
    }
  }

  @Test
  fun `runs job fails on retry`() {
    whenever(jobRepository.saveAndFlush(any())) doReturn job

    runner.runJobWithRetry(
      JobDefinition(JobType.ATTENDANCE_CREATE) {
        throw RuntimeException()
      },
    )

    with(job) {
      jobType isEqualTo JobType.ATTENDANCE_CREATE
      endedAt isCloseTo TimeSource.now()
      successful isBool false
    }
  }

  @Test
  fun `runs job with error`() {
    whenever(jobRepository.saveAndFlush(any())) doReturn job

    runner.runJob(JobDefinition(JobType.ATTENDANCE_CREATE) { throw RuntimeException("it failed") })

    with(job) {
      jobType isEqualTo JobType.ATTENDANCE_CREATE
      endedAt isCloseTo TimeSource.now()
      successful isBool false
    }

    verify(monitoringService).capture("Job 'ATTENDANCE_CREATE' with job id '1' failed")
  }

  @Test
  fun `runs a distributed job without error`() {
    val job = Job(jobId = 1, JobType.SCHEDULES)

    whenever(jobRepository.saveAndFlush(any())) doReturn job

    fun dummy(job: Job) {
      job.completedSubTasks = 1
    }

    runner.runDistributedJob(JobType.SCHEDULES, ::dummy)

    with(job) {
      jobType isEqualTo JobType.SCHEDULES
      endedAt isEqualTo null
      successful isBool false
      completedSubTasks isEqualTo 1
    }
  }

  @Test
  fun `runs a distributed job with error`() {
    val job = Job(jobId = 1, JobType.SCHEDULES)

    whenever(jobRepository.saveAndFlush(any())) doReturn job

    fun dummy(job: Job): Unit = throw RuntimeException("it failed")

    runner.runDistributedJob(JobType.SCHEDULES, ::dummy)

    with(job) {
      jobType isEqualTo JobType.SCHEDULES
      endedAt isCloseTo TimeSource.now()
      successful isBool false
      completedSubTasks isEqualTo null
    }

    verify(monitoringService).capture("Failed to start distributed job 'SCHEDULES' with job id '1'")
  }
}

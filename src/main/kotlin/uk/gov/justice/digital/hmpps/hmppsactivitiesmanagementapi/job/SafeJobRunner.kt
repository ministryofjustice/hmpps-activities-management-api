package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Retryable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MonitoringService
import java.time.LocalDateTime

@Component
class SafeJobRunner(
  private val jobRepository: JobRepository,
  private val monitoringService: MonitoringService,
  private val retryable: Retryable,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun runJob(jobDefinition: JobDefinition) {
    runSafe(jobDefinition, false)
  }

  fun runJobWithRetry(jobDefinition: JobDefinition) {
    runSafe(jobDefinition, true)
  }

  private fun runSafe(jobDefinition: JobDefinition, withRetry: Boolean = false): Result<Unit> {
    val startedAt = LocalDateTime.now()

    val job = jobRepository.saveAndFlush(Job(jobType = jobDefinition.jobType, startedAt = startedAt))

    log.info("JOB: Running job ${jobDefinition.jobType} with job id '${job.jobId}'")

    return runCatching {
      if (withRetry) {
        retryable.retry { jobDefinition.block() }
      } else {
        jobDefinition.block()
      }
    }
      .onSuccess { jobRepository.saveAndFlush(job.succeeded()) }
      .onFailure {
        log.error("JOB: Failed to run job ${jobDefinition.jobType} with job id '${job.jobId}'", it)
        jobRepository.saveAndFlush(job.failed()).also { failedJob ->
          monitoringService.capture("Job '${failedJob.jobType}' with job id '${failedJob.jobId}' failed")
        }
      }
  }
}

data class JobDefinition(val jobType: JobType, val block: () -> Unit)

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MonitoringService
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

@Component
class SafeJobRunner(private val jobRepository: JobRepository, private val monitoringService: MonitoringService) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun runJob(jobDefinition: JobDefinition) {
    runSafe(jobDefinition)
  }

  /**
   * This will run the dependent jobs in the order they are supplied.
   *
   * If a job in the sequence fails then the job(s) that follow (if there are any) will not run, this is intentional.
   */
  fun runDependentJobs(vararg jobDefinitions: JobDefinition) {
    val success = AtomicBoolean(true)

    jobDefinitions.forEach { job ->
      val elapsed = measureTimeMillis {
        if (success.get()) {
          runSafe(job)
            .onFailure {
              success.set(false)

              log.warn("JOB: Failure occurred running job ${job.jobType}")
            }
        } else {
          log.warn("JOB: Ignoring job ${job.jobType} due to failure in a dependent job.")

          jobRepository.saveAndFlush(Job.failed(job.jobType, LocalDateTime.now())).also { failedJob ->
            monitoringService.capture("Dependant job '${failedJob.jobType}' for job id '${failedJob.jobId}' failed")
          }
        }
      }
      log.info("JOB: Elapsed time for job ${job.jobType} ${elapsed}ms")
    }
  }

  private fun runSafe(jobDefinition: JobDefinition): Result<Unit> {
    val startedAt = LocalDateTime.now()

    return runCatching {
      jobDefinition.block()
    }
      .onSuccess { jobRepository.saveAndFlush(Job.successful(jobDefinition.jobType, startedAt)) }
      .onFailure {
        log.error("Failed to run ${jobDefinition.jobType} job", it)
        jobRepository.saveAndFlush(Job.failed(jobDefinition.jobType, startedAt)).also { failedJob ->
          monitoringService.capture("Job '${failedJob.jobType}' for job id '${failedJob.jobId}' failed")
        }
      }
  }
}

data class JobDefinition(val jobType: JobType, val block: () -> Unit)

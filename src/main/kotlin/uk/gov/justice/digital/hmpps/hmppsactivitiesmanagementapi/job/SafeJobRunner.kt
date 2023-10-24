package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

@Component
@Transactional
class SafeJobRunner(private val jobRepository: JobRepository, private val transactionHandler: TransactionHandler) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun runJob(jobDefinition: JobDefinition) {
    runSafe(jobDefinition).onFailure { throw RuntimeException("Failure occurred running job ${jobDefinition.jobType}") }
  }

  /**
   * This will run the dependent jobs in the order they are supplied.
   *
   * If a job in the sequence fails then the job(s) that follow (if there are any) will not run, this is intentional.
   */
  fun runDependentJobs(vararg jobDefinitions: JobDefinition) {
    val success = AtomicBoolean(true)
    var failedJob: JobDefinition? = null

    jobDefinitions.forEach { job ->
      if (success.get()) {
        runSafe(job)
          .onFailure {
            success.set(false)
            failedJob = job
          }
      } else {
        log.warn("Ignoring job ${job.jobType} due to failure in a dependent job.")

        transactionHandler.newSpringTransaction {
          jobRepository.saveAndFlush(Job.failed(job.jobType, LocalDateTime.now()))
        }
      }
    }

    if (!success.get()) {
      throw RuntimeException("Failure occurred running job ${failedJob!!.jobType}")
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

        transactionHandler.newSpringTransaction {
          jobRepository.saveAndFlush(Job.failed(jobDefinition.jobType, startedAt))
        }
      }
  }
}

data class JobDefinition(val jobType: JobType, val block: () -> Unit)

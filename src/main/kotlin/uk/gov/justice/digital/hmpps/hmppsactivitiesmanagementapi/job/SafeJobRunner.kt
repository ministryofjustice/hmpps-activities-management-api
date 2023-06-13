package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository

@Component
class SafeJobRunner(private val jobRepository: JobRepository) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun runSafe(vararg jobDefinitions: JobDefinition) {
    jobDefinitions.forEach(this::runSafe)
  }

  fun runSafe(jobDefinition: JobDefinition) {
    val job = Job.start(jobDefinition.jobType)

    runCatching {
      jobDefinition.block()
    }
      .onSuccess { job.succeeded() }
      .onFailure { log.error("Failed to run ${jobDefinition.jobType} job", it) }

    jobRepository.saveAndFlush(job)
  }
}

data class JobDefinition(val jobType: JobType, val block: () -> Unit)

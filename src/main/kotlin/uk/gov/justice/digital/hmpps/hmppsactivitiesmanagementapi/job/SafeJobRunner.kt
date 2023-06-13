package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import java.time.LocalDateTime

@Component
class SafeJobRunner(private val jobRepository: JobRepository) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun runSafe(vararg jobDefinitions: JobDefinition) {
    jobDefinitions.forEach(::runSafe)
  }

  fun runSafe(jobDefinition: JobDefinition) {
    val startedAt = LocalDateTime.now()

    runCatching {
      jobDefinition.block()
    }
      .onSuccess { jobRepository.saveAndFlush(Job.successful(jobDefinition.jobType, startedAt)) }
      .onFailure {
        log.error("Failed to run ${jobDefinition.jobType} job", it)

        jobRepository.saveAndFlush(Job.failed(jobDefinition.jobType, startedAt))
      }
  }
}

data class JobDefinition(val jobType: JobType, val block: () -> Unit)

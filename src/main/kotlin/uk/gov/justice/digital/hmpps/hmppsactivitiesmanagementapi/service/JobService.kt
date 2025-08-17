package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
class JobService(
  private val jobRepository: JobRepository,
) {
  fun initialiseCounts(jobId: Long, totalSubTasks: Int) {
    jobRepository.initialiseCounts(jobId, totalSubTasks)
  }

  fun incrementCount(jobId: Long) {
    jobRepository.incrementCount(jobId)
    jobRepository.findOrThrowNotFound(jobId).let { job ->
      if (job.completedSubTasks == job.totalSubTasks) {
        job.succeeded()
        jobRepository.saveAndFlush(job)
      }
    }
  }
}

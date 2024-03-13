package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MonitoringService

abstract class JobsTestBase {
  private val jobRepository: JobRepository = mock()
  val safeJobRunner = spy(SafeJobRunner(jobRepository, mock<MonitoringService>()) { it() })

  fun mockJobs(vararg jobTypes: JobType) {
    jobTypes.forEachIndexed { index, type ->
      whenever(jobRepository.saveAndFlush(argThat { jobType == type })) doReturn Job(
        jobId = index.toLong(),
        jobType = type,
        startedAt = TimeSource.now(),
      )
    }
  }

  fun verifyJobsWithRetryCalled(vararg jobTypes: JobType) {
    jobTypes.forEach { verify(safeJobRunner).runJobWithRetry(argThat { this.jobType == it }) }
    verifyNoMoreInteractions(safeJobRunner)
  }
}

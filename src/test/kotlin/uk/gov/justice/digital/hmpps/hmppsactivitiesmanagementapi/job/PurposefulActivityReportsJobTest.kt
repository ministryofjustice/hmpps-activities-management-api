package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.PURPOSEFUL_ACTIVITY_REPORTS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PurposefulActivityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.S3Service

@ExtendWith(MockitoExtension::class)
class PurposefulActivityReportsJobTest : JobsTestBase() {
  // private val service: ManageScheduledInstancesService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val purposefulActivityService: PurposefulActivityService = mock()
  private val s3Service: S3Service = mock()
  private val job = PurposefulActivityReportsJob(safeJobRunner, purposefulActivityService, s3Service)

  @Test
  fun `Create purposeful activity reports`() {
    mockJobs(PURPOSEFUL_ACTIVITY_REPORTS)

    runBlocking {
      job.execute(1)
    }

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())
    // verify(service).create()

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(PURPOSEFUL_ACTIVITY_REPORTS)
  }
}

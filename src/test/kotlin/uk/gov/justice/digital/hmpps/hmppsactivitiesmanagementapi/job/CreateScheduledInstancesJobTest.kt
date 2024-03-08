package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.SCHEDULES
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesService

@ExtendWith(MockitoExtension::class)
class CreateScheduledInstancesJobTest : JobsTestBase() {
  private val service: ManageScheduledInstancesService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = CreateScheduledInstancesJob(service, safeJobRunner)

  @Test
  fun `create scheduled instances is triggered`() {
    mockJobs(SCHEDULES)

    job.execute()

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())
    verify(service).create()

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(SCHEDULES)
  }
}

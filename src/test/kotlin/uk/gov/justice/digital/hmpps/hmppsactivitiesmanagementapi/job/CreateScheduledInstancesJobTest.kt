package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.SCHEDULES
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesService

@ExtendWith(MockitoExtension::class)
class CreateScheduledInstancesJobTest : JobsTestBase() {
  private val service: ManageScheduledInstancesService = mock()

  @Test
  fun `distributed create scheduled instances is triggered`() {
    val job = CreateScheduledInstancesJob(service, safeJobRunner)

    mockJobs(SCHEDULES)

    job.execute()

    verify(safeJobRunner).runDistributedJob(SCHEDULES, service::sendCreateSchedulesEvents)
  }
}

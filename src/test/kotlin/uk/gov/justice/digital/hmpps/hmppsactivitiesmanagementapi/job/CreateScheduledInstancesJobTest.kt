package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.SCHEDULES
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MonitoringService

@ExtendWith(MockitoExtension::class)
class CreateScheduledInstancesJobTest {
  private val service: ManageScheduledInstancesService = mock()
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository, mock<MonitoringService>()))
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = CreateScheduledInstancesJob(service, safeJobRunner)

  @Test
  fun `create scheduled instances is triggered`() {
    job.execute()

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())
    verify(service).create()

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(SCHEDULES)
  }
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.SCHEDULES
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesService

@ExtendWith(MockitoExtension::class)
class CreateScheduledInstancesJobTest : JobsTestBase() {
  private val service: ManageScheduledInstancesService = mock()
  private val featureSwitches: FeatureSwitches = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()

  @Test
  fun `create scheduled instances is triggered`() {
    whenever(featureSwitches.isEnabled(any<Feature>(), any())).thenReturn(false)

    val job = CreateScheduledInstancesJob(service, safeJobRunner, featureSwitches)

    mockJobs(SCHEDULES)

    job.execute()

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    verifyNoMoreInteractions(safeJobRunner)

    verify(service).create()

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(SCHEDULES)
  }

  @Test
  fun `distributed create scheduled instances is triggered`() {
    whenever(featureSwitches.isEnabled(any<Feature>(), any())).thenReturn(true)

    val job = CreateScheduledInstancesJob(service, safeJobRunner, featureSwitches)

    mockJobs(SCHEDULES)

    job.execute()

    verify(safeJobRunner).runDistributedJob(SCHEDULES, service::sendCreateSchedulesEvents)

    verify(service, never()).create()
  }
}

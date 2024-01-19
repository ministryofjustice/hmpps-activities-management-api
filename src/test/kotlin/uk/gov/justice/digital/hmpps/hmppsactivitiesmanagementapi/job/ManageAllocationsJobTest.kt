package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MonitoringService
import java.time.LocalDate

class ManageAllocationsJobTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock {
    on { findAll() } doReturn listOf(rolloutPrison(PENTONVILLE_PRISON_CODE), rolloutPrison(MOORLAND_PRISON_CODE))
  }
  private val deallocationService: ManageAllocationsService = mock()
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository, mock<MonitoringService>()))
  private val job = ManageAllocationsJob(rolloutPrisonRepository, deallocationService, safeJobRunner)
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()

  @Test
  fun `activate allocation operation triggered`() {
    job.execute(withActivate = true)

    verify(deallocationService).allocations(AllocationOperation.STARTING_TODAY)
    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.ALLOCATE)
  }

  @Test
  fun `deallocate allocation operation triggered`() {
    job.execute(withDeallocate = true)

    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, LocalDate.now())
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, LocalDate.now())
    verify(deallocationService).allocations(AllocationOperation.EXPIRING_TODAY)
    verify(safeJobRunner, times(2)).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.DEALLOCATE_ENDING)
    assertThat(jobDefinitionCaptor.secondValue.jobType).isEqualTo(JobType.DEALLOCATE_EXPIRING)
  }

  @Test
  fun `activate and deallocate allocation operations triggered`() {
    job.execute(withActivate = true, withDeallocate = true)

    verify(deallocationService).allocations(AllocationOperation.STARTING_TODAY)
    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, LocalDate.now())
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, LocalDate.now())
    verify(deallocationService).allocations(AllocationOperation.EXPIRING_TODAY)
    verify(safeJobRunner, times(3)).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.ALLOCATE)
    assertThat(jobDefinitionCaptor.secondValue.jobType).isEqualTo(JobType.DEALLOCATE_ENDING)
    assertThat(jobDefinitionCaptor.thirdValue.jobType).isEqualTo(JobType.DEALLOCATE_EXPIRING)
  }
}

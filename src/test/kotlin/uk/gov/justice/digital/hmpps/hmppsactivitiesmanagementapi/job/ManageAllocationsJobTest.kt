package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.daysAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MonitoringService

class ManageAllocationsJobTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock {
    on { findAll() } doReturn listOf(rolloutPrison(PENTONVILLE_PRISON_CODE), rolloutPrison(MOORLAND_PRISON_CODE))
  }
  private val deallocationService: ManageAllocationsService = mock()
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository, mock<MonitoringService>()))
  private val job = ManageAllocationsJob(rolloutPrisonRepository, deallocationService, safeJobRunner, 2)
  private val yesterday = 1.daysAgo()
  private val twoDaysAgo = 2.daysAgo()

  @Test
  fun `activate allocation operation triggered`() {
    job.execute(withActivate = true)

    verify(deallocationService).allocations(AllocationOperation.STARTING_TODAY)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsCalled(JobType.ALLOCATE, JobType.START_SUSPENSIONS, JobType.END_SUSPENSIONS)
  }

  @Test
  fun `deallocate allocation due to end operation triggered`() {
    job.execute(withDeallocateEnding = true)

    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, twoDaysAgo)
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, twoDaysAgo)
    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, yesterday)
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, yesterday)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsCalled(JobType.DEALLOCATE_ENDING)
  }

  @Test
  fun `deallocate allocation due to expire operation triggered`() {
    job.execute(withDeallocateExpiring = true)

    verify(deallocationService).allocations(AllocationOperation.EXPIRING_TODAY)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsCalled(JobType.DEALLOCATE_EXPIRING)
  }

  @Test
  fun `activate and deallocate allocation due to end operations triggered`() {
    job.execute(withActivate = true, withDeallocateEnding = true)

    verify(deallocationService).allocations(AllocationOperation.STARTING_TODAY)
    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, twoDaysAgo)
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, twoDaysAgo)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, yesterday)
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, yesterday)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsCalled(JobType.ALLOCATE, JobType.START_SUSPENSIONS, JobType.END_SUSPENSIONS, JobType.DEALLOCATE_ENDING)
  }

  @Test
  fun `activate and deallocate allocation due to expire operations triggered`() {
    job.execute(withActivate = true, withDeallocateExpiring = true)

    verify(deallocationService).allocations(AllocationOperation.STARTING_TODAY)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).allocations(AllocationOperation.EXPIRING_TODAY)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsCalled(JobType.ALLOCATE, JobType.START_SUSPENSIONS, JobType.END_SUSPENSIONS, JobType.DEALLOCATE_EXPIRING)
  }

  @Test
  fun `deallocate allocation due to end and deallocate allocation due to expire operations triggered`() {
    job.execute(withDeallocateEnding = true, withDeallocateExpiring = true)

    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, twoDaysAgo)
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, twoDaysAgo)
    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, yesterday)
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, yesterday)
    verify(deallocationService).allocations(AllocationOperation.EXPIRING_TODAY)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsCalled(JobType.DEALLOCATE_ENDING, JobType.DEALLOCATE_EXPIRING)
  }

  @Test
  fun `activate, deallocate allocation due to end and deallocate allocation due to expire operations triggered`() {
    job.execute(withActivate = true, withDeallocateEnding = true, withDeallocateExpiring = true)

    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, twoDaysAgo)
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, twoDaysAgo)
    verify(deallocationService).allocations(AllocationOperation.STARTING_TODAY)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, yesterday)
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, yesterday)
    verify(deallocationService).allocations(AllocationOperation.EXPIRING_TODAY)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsCalled(JobType.ALLOCATE, JobType.START_SUSPENSIONS, JobType.END_SUSPENSIONS, JobType.DEALLOCATE_ENDING, JobType.DEALLOCATE_EXPIRING)
  }

  private fun verifyJobsCalled(vararg jobTypes: JobType) {
    jobTypes.forEach { verify(safeJobRunner).runJob(argThat { this.jobType == it }) }
    verifyNoMoreInteractions(safeJobRunner)
  }
}

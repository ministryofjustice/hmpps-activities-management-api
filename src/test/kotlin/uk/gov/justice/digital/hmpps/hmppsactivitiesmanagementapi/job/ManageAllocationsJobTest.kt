package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.daysAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ALLOCATE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.DEALLOCATE_ENDING
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.DEALLOCATE_EXPIRING
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.END_SUSPENSIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.START_SUSPENSIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService

class ManageAllocationsJobTest : JobsTestBase() {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock {
    on { findAll() } doReturn listOf(rolloutPrison(PENTONVILLE_PRISON_CODE), rolloutPrison(MOORLAND_PRISON_CODE))
  }
  private val deallocationService: ManageAllocationsService = mock()
  private val job = ManageAllocationsJob(rolloutPrisonRepository, deallocationService, safeJobRunner, 2)
  private val yesterday = 1.daysAgo()
  private val twoDaysAgo = 2.daysAgo()

  @Test
  fun `activate allocation operation triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS)

    job.execute(withActivate = true)

    verify(deallocationService).allocations(AllocationOperation.STARTING_TODAY)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsWithRetryCalled(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS)
  }

  @Test
  fun `deallocate allocation due to end operation triggered`() {
    mockJobs(DEALLOCATE_ENDING)

    job.execute(withDeallocateEnding = true)

    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, twoDaysAgo)
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, twoDaysAgo)
    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, yesterday)
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, yesterday)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsWithRetryCalled(DEALLOCATE_ENDING)
  }

  @Test
  fun `deallocate allocation due to expire operation triggered`() {
    mockJobs(DEALLOCATE_EXPIRING)

    job.execute(withDeallocateExpiring = true)

    verify(deallocationService).allocations(AllocationOperation.EXPIRING_TODAY)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsWithRetryCalled(DEALLOCATE_EXPIRING)
  }

  @Test
  fun `activate and deallocate allocation due to end operations triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_ENDING)

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

    verifyJobsWithRetryCalled(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_ENDING)
  }

  @Test
  fun `activate and deallocate allocation due to expire operations triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_EXPIRING)

    job.execute(withActivate = true, withDeallocateExpiring = true)

    verify(deallocationService).allocations(AllocationOperation.STARTING_TODAY)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).allocations(AllocationOperation.EXPIRING_TODAY)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsWithRetryCalled(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_EXPIRING)
  }

  @Test
  fun `deallocate allocation due to end and deallocate allocation due to expire operations triggered`() {
    mockJobs(DEALLOCATE_ENDING, DEALLOCATE_EXPIRING)

    job.execute(withDeallocateEnding = true, withDeallocateExpiring = true)

    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, twoDaysAgo)
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, twoDaysAgo)
    verify(deallocationService).endAllocationsDueToEnd(PENTONVILLE_PRISON_CODE, yesterday)
    verify(deallocationService).endAllocationsDueToEnd(MOORLAND_PRISON_CODE, yesterday)
    verify(deallocationService).allocations(AllocationOperation.EXPIRING_TODAY)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsWithRetryCalled(DEALLOCATE_ENDING, DEALLOCATE_EXPIRING)
  }

  @Test
  fun `activate, deallocate allocation due to end and deallocate allocation due to expire operations triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_ENDING, DEALLOCATE_EXPIRING)

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

    verifyJobsWithRetryCalled(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_ENDING, DEALLOCATE_EXPIRING)
  }
}

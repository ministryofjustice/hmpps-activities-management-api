package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.ALLOCATE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.DEALLOCATE_ENDING
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.DEALLOCATE_EXPIRING
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.END_SUSPENSIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.FIX_STUCK_AUTO_SUSPENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.START_SUSPENSIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToEndService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToExpireService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageNewAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.SuspendAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.UnsuspendAllocationsService

class ManageAllocationsJobTest : JobsTestBase() {
  private val manageAllocationsService: ManageAllocationsService = mock()
  private val manageAllocationsDueToEndService: ManageAllocationsDueToEndService = mock()
  private val manageAllocationsDueToExpireService: ManageAllocationsDueToExpireService = mock()
  private val manageNewAllocationsService: ManageNewAllocationsService = mock()
  private val suspendAllocationsService: SuspendAllocationsService = mock()
  private val unsuspendAllocationsService: UnsuspendAllocationsService = mock()
  private val newAllocationsCaptor = argumentCaptor<Job>()
  private val allocationsDueToEndCaptor = argumentCaptor<Job>()
  private val allocationsDueToExpireCaptor = argumentCaptor<Job>()

  private val job = manageAllocationsJobs()

  @Test
  fun `activate, suspend and unsuspend allocations operations triggered with allocate`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS)

    manageAllocationsJobs().execute(withActivate = true)

    verify(safeJobRunner).runDistributedJob(ALLOCATE, manageNewAllocationsService::sendAllocationEvents)
    verify(manageNewAllocationsService).sendAllocationEvents(newAllocationsCaptor.capture())
    assertThat(newAllocationsCaptor.firstValue.jobType).isEqualTo(ALLOCATE)

    verifyNoMoreInteractions(manageNewAllocationsService)
    verifyNoMoreInteractions(manageAllocationsService)
    verifyNoInteractions(manageAllocationsDueToEndService)
    verifyNoInteractions(manageAllocationsDueToExpireService)
    verifyNoInteractions(suspendAllocationsService)
    verifyNoInteractions(unsuspendAllocationsService)
  }

  @Test
  fun `deallocate allocations due to end operation triggered`() {
    mockJobs(DEALLOCATE_ENDING)

    manageAllocationsJobs().execute(withDeallocateEnding = true)

    verify(safeJobRunner).runDistributedJob(DEALLOCATE_ENDING, manageAllocationsDueToEndService::sendAllocationsDueToEndEvents)
    verify(manageAllocationsDueToEndService).sendAllocationsDueToEndEvents(allocationsDueToEndCaptor.capture())
    assertThat(allocationsDueToEndCaptor.firstValue.jobType).isEqualTo(DEALLOCATE_ENDING)

    verifyNoMoreInteractions(manageAllocationsDueToEndService)
    verifyNoInteractions(manageAllocationsService)
    verifyNoInteractions(manageNewAllocationsService)
    verifyNoInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled()
  }

  @Test
  fun `deallocate allocations due to expire operation triggered`() {
    mockJobs(DEALLOCATE_EXPIRING)

    manageAllocationsJobs().execute(withDeallocateExpiring = true)

    verify(safeJobRunner).runDistributedJob(DEALLOCATE_EXPIRING, manageAllocationsDueToExpireService::sendAllocationsDueToExpireEvents)
    verify(manageAllocationsDueToExpireService).sendAllocationsDueToExpireEvents(allocationsDueToExpireCaptor.capture())
    assertThat(allocationsDueToExpireCaptor.firstValue.jobType).isEqualTo(DEALLOCATE_EXPIRING)

    verifyNoMoreInteractions(manageAllocationsDueToExpireService)
    verifyNoInteractions(manageAllocationsService)
    verifyNoInteractions(manageNewAllocationsService)
    verifyNoInteractions(manageAllocationsDueToEndService)

    verifyJobsWithRetryCalled()
  }

  @Test
  fun `fix auto suspended operation triggered`() {
    mockJobs(FIX_STUCK_AUTO_SUSPENDED)

    job.execute(withFixAutoSuspended = true)

    verify(manageAllocationsService).fixPrisonersIncorrectlyAutoSuspended()

    verifyNoMoreInteractions(manageAllocationsService)
    verifyNoInteractions(manageNewAllocationsService)
    verifyNoInteractions(manageAllocationsDueToEndService)
    verifyNoInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled(FIX_STUCK_AUTO_SUSPENDED)
  }

  @Test
  fun `activate and deallocate allocations due to end operations triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_ENDING)

    manageAllocationsJobs().execute(withActivate = true, withDeallocateEnding = true)

    verify(safeJobRunner).runDistributedJob(ALLOCATE, manageNewAllocationsService::sendAllocationEvents)
    verify(manageNewAllocationsService).sendAllocationEvents(newAllocationsCaptor.capture())
    assertThat(newAllocationsCaptor.firstValue.jobType).isEqualTo(ALLOCATE)

    verify(safeJobRunner).runDistributedJob(DEALLOCATE_ENDING, manageAllocationsDueToEndService::sendAllocationsDueToEndEvents)
    verify(manageAllocationsDueToEndService).sendAllocationsDueToEndEvents(allocationsDueToEndCaptor.capture())
    assertThat(allocationsDueToEndCaptor.firstValue.jobType).isEqualTo(DEALLOCATE_ENDING)

    verifyNoMoreInteractions(manageAllocationsService)
    verifyNoMoreInteractions(manageNewAllocationsService)
    verifyNoMoreInteractions(manageAllocationsDueToEndService)
    verifyNoMoreInteractions(manageAllocationsDueToExpireService)
    verifyNoMoreInteractions(suspendAllocationsService)
    verifyNoMoreInteractions(unsuspendAllocationsService)

    verifyJobsWithRetryCalled()
  }

  @Test
  fun `activate, start suspensions, end suspensions and deallocate allocations due to expire operations triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_EXPIRING)

    manageAllocationsJobs().execute(withActivate = true, withDeallocateExpiring = true)

    verify(safeJobRunner).runDistributedJob(ALLOCATE, manageNewAllocationsService::sendAllocationEvents)
    verify(manageNewAllocationsService).sendAllocationEvents(newAllocationsCaptor.capture())
    assertThat(newAllocationsCaptor.firstValue.jobType).isEqualTo(ALLOCATE)

    verify(safeJobRunner).runDistributedJob(DEALLOCATE_EXPIRING, manageAllocationsDueToExpireService::sendAllocationsDueToExpireEvents)
    verify(manageAllocationsDueToExpireService).sendAllocationsDueToExpireEvents(allocationsDueToExpireCaptor.capture())
    assertThat(allocationsDueToExpireCaptor.firstValue.jobType).isEqualTo(DEALLOCATE_EXPIRING)

    verifyNoMoreInteractions(manageAllocationsService)
    verifyNoMoreInteractions(manageNewAllocationsService)
    verifyNoMoreInteractions(manageAllocationsDueToExpireService)
    verifyNoInteractions(manageAllocationsDueToEndService)
    verifyNoInteractions(suspendAllocationsService)
    verifyNoInteractions(unsuspendAllocationsService)

    verifyJobsWithRetryCalled()
  }

  @Test
  fun `deallocate allocations due to end and deallocate allocations due to expire operations triggered`() {
    mockJobs(DEALLOCATE_ENDING, DEALLOCATE_EXPIRING)

    manageAllocationsJobs().execute(withDeallocateEnding = true, withDeallocateExpiring = true)

    verify(safeJobRunner).runDistributedJob(DEALLOCATE_ENDING, manageAllocationsDueToEndService::sendAllocationsDueToEndEvents)
    verify(manageAllocationsDueToEndService).sendAllocationsDueToEndEvents(allocationsDueToEndCaptor.capture())
    assertThat(allocationsDueToEndCaptor.firstValue.jobType).isEqualTo(DEALLOCATE_ENDING)

    verify(safeJobRunner).runDistributedJob(DEALLOCATE_EXPIRING, manageAllocationsDueToExpireService::sendAllocationsDueToExpireEvents)
    verify(manageAllocationsDueToExpireService).sendAllocationsDueToExpireEvents(allocationsDueToExpireCaptor.capture())
    assertThat(allocationsDueToExpireCaptor.firstValue.jobType).isEqualTo(DEALLOCATE_EXPIRING)

    verifyNoInteractions(manageAllocationsService)
    verifyNoInteractions(manageNewAllocationsService)
    verifyNoMoreInteractions(manageAllocationsDueToEndService)
    verifyNoMoreInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled()
  }

  @Test
  fun `activate and fix auto suspended allocations operations triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, FIX_STUCK_AUTO_SUSPENDED)

    manageAllocationsJobs().execute(withActivate = true, withFixAutoSuspended = true)

    verify(safeJobRunner).runDistributedJob(ALLOCATE, manageNewAllocationsService::sendAllocationEvents)
    verify(manageNewAllocationsService).sendAllocationEvents(newAllocationsCaptor.capture())
    assertThat(newAllocationsCaptor.firstValue.jobType).isEqualTo(ALLOCATE)

    verify(manageAllocationsService).fixPrisonersIncorrectlyAutoSuspended()

    verifyNoMoreInteractions(manageAllocationsService)
    verifyNoMoreInteractions(manageNewAllocationsService)
    verifyNoInteractions(manageAllocationsDueToEndService)
    verifyNoInteractions(manageAllocationsDueToExpireService)
    verifyNoInteractions(suspendAllocationsService)
    verifyNoInteractions(unsuspendAllocationsService)

    verifyJobsWithRetryCalled(FIX_STUCK_AUTO_SUSPENDED)
  }

  @Test
  fun `activate, deallocate allocation due to end, deallocate allocation due to expire and fix auto-suspended operations triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, FIX_STUCK_AUTO_SUSPENDED, DEALLOCATE_ENDING, DEALLOCATE_EXPIRING)

    manageAllocationsJobs().execute(withActivate = true, withDeallocateEnding = true, withDeallocateExpiring = true, withFixAutoSuspended = true)

    verify(safeJobRunner).runDistributedJob(ALLOCATE, manageNewAllocationsService::sendAllocationEvents)
    verify(manageNewAllocationsService).sendAllocationEvents(newAllocationsCaptor.capture())
    assertThat(newAllocationsCaptor.firstValue.jobType).isEqualTo(ALLOCATE)

    verify(safeJobRunner).runDistributedJob(DEALLOCATE_ENDING, manageAllocationsDueToEndService::sendAllocationsDueToEndEvents)
    verify(manageAllocationsDueToEndService).sendAllocationsDueToEndEvents(allocationsDueToEndCaptor.capture())
    assertThat(allocationsDueToEndCaptor.firstValue.jobType).isEqualTo(DEALLOCATE_ENDING)

    verify(safeJobRunner).runDistributedJob(DEALLOCATE_EXPIRING, manageAllocationsDueToExpireService::sendAllocationsDueToExpireEvents)
    verify(manageAllocationsDueToExpireService).sendAllocationsDueToExpireEvents(allocationsDueToExpireCaptor.capture())
    assertThat(allocationsDueToExpireCaptor.firstValue.jobType).isEqualTo(DEALLOCATE_EXPIRING)

    verify(manageAllocationsService).fixPrisonersIncorrectlyAutoSuspended()

    verifyNoMoreInteractions(manageAllocationsService)
    verifyNoMoreInteractions(manageNewAllocationsService)
    verifyNoMoreInteractions(manageAllocationsDueToEndService)
    verifyNoMoreInteractions(manageAllocationsDueToExpireService)
    verifyNoMoreInteractions(unsuspendAllocationsService)
    verifyNoMoreInteractions(manageAllocationsService)

    verifyJobsWithRetryCalled(FIX_STUCK_AUTO_SUSPENDED)
  }

  fun manageAllocationsJobs() = ManageAllocationsJob(
    manageAllocationsService,
    manageAllocationsDueToEndService,
    manageAllocationsDueToExpireService,
    manageNewAllocationsService,
    safeJobRunner,
  )
}

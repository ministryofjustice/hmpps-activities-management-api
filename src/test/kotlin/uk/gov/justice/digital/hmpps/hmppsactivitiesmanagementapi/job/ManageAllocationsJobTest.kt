package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
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
  private val featureSwitches: FeatureSwitches = mock()
  private val newAllocationsCaptor = argumentCaptor<Job>()
  private val allocationsDueToEndCaptor = argumentCaptor<Job>()
  private val allocationsDueToExpireCaptor = argumentCaptor<Job>()

  private val job = manageAllocationsJobs()

  @Test
  fun `activate, suspend and unsuspend allocations operations triggered without SQS`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS)

    job.execute(withActivate = true)

    verify(manageNewAllocationsService).allocations()
    verify(suspendAllocationsService).suspendAllocationsDueToBeSuspended()
    verify(unsuspendAllocationsService).unsuspendAllocationsDueToBeUnsuspended()
    verifyNoMoreInteractions(manageNewAllocationsService)
    verifyNoMoreInteractions(manageAllocationsService)
    verifyNoInteractions(manageAllocationsDueToEndService)
    verifyNoInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS)
  }

  @Test
  fun `activate, suspend and unsuspend allocations operations triggered with allocate with SQS`() {
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_ACTIVATE_ALLOCATIONS_ENABLED)).thenReturn(true)

    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS)

    manageAllocationsJobs(featureSwitches).execute(withActivate = true)

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
  fun `deallocate allocations due to end operation triggered without SQS`() {
    mockJobs(DEALLOCATE_ENDING)

    job.execute(withDeallocateEnding = true)

    verify(manageAllocationsDueToEndService).endAllocationsDueToEnd()
    verifyNoMoreInteractions(manageAllocationsDueToEndService)
    verifyNoInteractions(manageAllocationsService)
    verifyNoInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled(DEALLOCATE_ENDING)
  }

  @Test
  fun `deallocate allocations due to end operation triggered with SQS`() {
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_ENDING_ENABLED)).thenReturn(true)

    mockJobs(DEALLOCATE_ENDING)

    manageAllocationsJobs(featureSwitches).execute(withDeallocateEnding = true)

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
  fun `deallocate allocations due to expire operation triggered without SQS`() {
    mockJobs(DEALLOCATE_EXPIRING)

    job.execute(withDeallocateExpiring = true)

    verify(manageAllocationsDueToExpireService).deallocateAllocationsDueToExpire()
    verifyNoInteractions(manageAllocationsService)
    verifyNoInteractions(manageNewAllocationsService)
    verifyNoInteractions(manageAllocationsDueToEndService)
    verifyNoMoreInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled(DEALLOCATE_EXPIRING)
  }

  @Test
  fun `deallocate allocations due to expire operation triggered with SQS`() {
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_EXPIRING_ENABLED)).thenReturn(true)

    mockJobs(DEALLOCATE_EXPIRING)

    manageAllocationsJobs(featureSwitches).execute(withDeallocateExpiring = true)

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
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_ACTIVATE_ALLOCATIONS_ENABLED)).thenReturn(true)
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_ENDING_ENABLED)).thenReturn(true)

    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_ENDING)

    manageAllocationsJobs(featureSwitches).execute(withActivate = true, withDeallocateEnding = true)

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
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_ACTIVATE_ALLOCATIONS_ENABLED)).thenReturn(true)
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_EXPIRING_ENABLED)).thenReturn(true)

    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_EXPIRING)

    manageAllocationsJobs(featureSwitches).execute(withActivate = true, withDeallocateExpiring = true)

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
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_ENDING_ENABLED)).thenReturn(true)
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_EXPIRING_ENABLED)).thenReturn(true)

    mockJobs(DEALLOCATE_ENDING, DEALLOCATE_EXPIRING)

    manageAllocationsJobs(featureSwitches).execute(withDeallocateEnding = true, withDeallocateExpiring = true)

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
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_ACTIVATE_ALLOCATIONS_ENABLED)).thenReturn(true)

    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, FIX_STUCK_AUTO_SUSPENDED)

    manageAllocationsJobs(featureSwitches).execute(withActivate = true, withFixAutoSuspended = true)

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
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_ACTIVATE_ALLOCATIONS_ENABLED)).thenReturn(true)
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_ENDING_ENABLED)).thenReturn(true)
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_EXPIRING_ENABLED)).thenReturn(true)

    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, FIX_STUCK_AUTO_SUSPENDED, DEALLOCATE_ENDING, DEALLOCATE_EXPIRING)

    manageAllocationsJobs(featureSwitches).execute(withActivate = true, withDeallocateEnding = true, withDeallocateExpiring = true, withFixAutoSuspended = true)

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

  fun manageAllocationsJobs(featureSwitches: FeatureSwitches = this.featureSwitches) = ManageAllocationsJob(
    manageAllocationsService,
    manageAllocationsDueToEndService,
    manageAllocationsDueToExpireService,
    manageNewAllocationsService,
    suspendAllocationsService,
    unsuspendAllocationsService,
    safeJobRunner,
    featureSwitches,
  )
}

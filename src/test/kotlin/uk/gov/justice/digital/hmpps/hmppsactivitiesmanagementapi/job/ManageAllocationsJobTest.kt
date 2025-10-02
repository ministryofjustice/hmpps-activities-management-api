package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToEndService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToExpireService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService

class ManageAllocationsJobTest : JobsTestBase() {
  private val rolloutPrisonService: RolloutPrisonService = mock {
    on { getRolloutPrisons() } doReturn listOf(rolloutPrison(PENTONVILLE_PRISON_CODE), rolloutPrison(MOORLAND_PRISON_CODE))
  }
  private val deallocationService: ManageAllocationsService = mock()
  private val manageAllocationsDueToEndService: ManageAllocationsDueToEndService = mock()
  private val manageAllocationsDueToExpireService: ManageAllocationsDueToExpireService = mock()
  private val featureSwitches: FeatureSwitches = mock()
  private val jobCaptor = argumentCaptor<Job>()

  private val job = ManageAllocationsJob(
    rolloutPrisonService,
    deallocationService,
    manageAllocationsDueToEndService,
    manageAllocationsDueToExpireService,
    safeJobRunner,
    featureSwitches,
  )

  @Test
  fun `activate allocation operation triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS)

    job.execute(withActivate = true)

    verify(deallocationService).allocations()
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verifyNoMoreInteractions(deallocationService)

    verifyJobsWithRetryCalled(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS)
  }

  @Test
  fun `deallocate allocation due to end operation triggered without SQS`() {
    mockJobs(DEALLOCATE_ENDING)

    job.execute(withDeallocateEnding = true)

    verifyJobsWithRetryCalled(DEALLOCATE_ENDING)
    verify(manageAllocationsDueToEndService).endAllocationsDueToEnd()
    verifyNoMoreInteractions(manageAllocationsDueToEndService)
    verifyNoInteractions(deallocationService)
    verifyNoInteractions(manageAllocationsDueToExpireService)
  }

  @Test
  fun `deallocate allocation due to end operation triggered with SQS`() {
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_ENDING_ENABLED)).thenReturn(true)

    mockJobs(DEALLOCATE_ENDING)

    ManageAllocationsJob(
      rolloutPrisonService,
      deallocationService,
      manageAllocationsDueToEndService,
      manageAllocationsDueToExpireService,
      safeJobRunner,
      featureSwitches,
    )
      .execute(withDeallocateEnding = true)

    verify(safeJobRunner).runDistributedJob(DEALLOCATE_ENDING, manageAllocationsDueToEndService::sendAllocationsDueToEndEvents)
    verify(manageAllocationsDueToEndService).sendAllocationsDueToEndEvents(jobCaptor.capture())
    assertThat(jobCaptor.firstValue.jobType).isEqualTo(DEALLOCATE_ENDING)
    verifyNoMoreInteractions(manageAllocationsDueToEndService)
    verifyNoInteractions(deallocationService)
    verifyNoInteractions(manageAllocationsDueToExpireService)
  }

  @Test
  fun `deallocate allocation due to expire operation triggered without SQS`() {
    mockJobs(DEALLOCATE_EXPIRING)

    job.execute(withDeallocateExpiring = true)

    verify(manageAllocationsDueToExpireService).deallocateAllocationsDueToExpire()
    verifyNoInteractions(deallocationService)
    verifyNoInteractions(manageAllocationsDueToEndService)
    verifyNoMoreInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled(DEALLOCATE_EXPIRING)
  }

  @Test
  fun `deallocate allocation due to expire operation triggered with SQS`() {
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_EXPIRING_ENABLED)).thenReturn(true)

    mockJobs(DEALLOCATE_EXPIRING)

    ManageAllocationsJob(
      rolloutPrisonService,
      deallocationService,
      manageAllocationsDueToEndService,
      manageAllocationsDueToExpireService,
      safeJobRunner,
      featureSwitches,
    )
      .execute(withDeallocateExpiring = true)

    verify(safeJobRunner).runDistributedJob(DEALLOCATE_EXPIRING, manageAllocationsDueToExpireService::sendAllocationsDueToExpireEvents)
    verify(manageAllocationsDueToExpireService).sendAllocationsDueToExpireEvents(jobCaptor.capture())
    assertThat(jobCaptor.firstValue.jobType).isEqualTo(DEALLOCATE_EXPIRING)
    verifyNoMoreInteractions(manageAllocationsDueToExpireService)
    verifyNoInteractions(deallocationService)
    verifyNoInteractions(manageAllocationsDueToEndService)
  }

  @Test
  fun `fix auto suspended operation triggered`() {
    mockJobs(FIX_STUCK_AUTO_SUSPENDED)

    job.execute(withFixAutoSuspended = true)

    verify(deallocationService).fixPrisonersIncorrectlyAutoSuspended()

    verifyNoMoreInteractions(deallocationService)

    verifyJobsWithRetryCalled(FIX_STUCK_AUTO_SUSPENDED)
  }

  @Test
  fun `activate and deallocate allocation due to end operations triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_ENDING)

    job.execute(withActivate = true, withDeallocateEnding = true)

    verify(deallocationService).allocations()
    verify(manageAllocationsDueToEndService).endAllocationsDueToEnd()
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verifyNoMoreInteractions(deallocationService)
    verifyNoMoreInteractions(manageAllocationsDueToEndService)
    verifyNoInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_ENDING)
  }

  @Test
  fun `activate and deallocate allocation due to end operations triggered with distributed DEALLOCATE_ENDING`() {
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_ENDING_ENABLED)).thenReturn(true)

    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_ENDING)

    ManageAllocationsJob(
      rolloutPrisonService,
      deallocationService,
      manageAllocationsDueToEndService,
      manageAllocationsDueToExpireService,
      safeJobRunner,
      featureSwitches,
    )
      .execute(withActivate = true, withDeallocateEnding = true)

    verify(deallocationService).allocations()
    verify(safeJobRunner).runDistributedJob(DEALLOCATE_ENDING, manageAllocationsDueToEndService::sendAllocationsDueToEndEvents)
    verify(manageAllocationsDueToEndService).sendAllocationsDueToEndEvents(jobCaptor.capture())
    assertThat(jobCaptor.firstValue.jobType).isEqualTo(DEALLOCATE_ENDING)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verifyNoMoreInteractions(deallocationService)
    verifyNoMoreInteractions(manageAllocationsDueToEndService)
    verifyNoMoreInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS)
  }

  @Test
  fun `activate and deallocate allocation due to expire operations triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_EXPIRING)

    job.execute(withActivate = true, withDeallocateExpiring = true)

    verify(deallocationService).allocations()
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verify(manageAllocationsDueToExpireService).deallocateAllocationsDueToExpire()
    verifyNoMoreInteractions(deallocationService)
    verifyNoMoreInteractions(manageAllocationsDueToExpireService)
    verifyNoInteractions(manageAllocationsDueToEndService)

    verifyJobsWithRetryCalled(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_EXPIRING)
  }

  @Test
  fun `deallocate allocation due to end and deallocate allocation due to expire operations triggered`() {
    mockJobs(DEALLOCATE_ENDING, DEALLOCATE_EXPIRING)

    job.execute(withDeallocateEnding = true, withDeallocateExpiring = true)

    verify(manageAllocationsDueToEndService).endAllocationsDueToEnd()
    verify(manageAllocationsDueToExpireService).deallocateAllocationsDueToExpire()
    verifyNoInteractions(deallocationService)
    verifyNoMoreInteractions(manageAllocationsDueToEndService)
    verifyNoMoreInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled(DEALLOCATE_ENDING, DEALLOCATE_EXPIRING)
  }

  @Test
  fun `activate and fix auto suspended operations triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, FIX_STUCK_AUTO_SUSPENDED)

    job.execute(withActivate = true, withFixAutoSuspended = true)

    verify(deallocationService).allocations()
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).fixPrisonersIncorrectlyAutoSuspended()
    verifyNoMoreInteractions(deallocationService)
    verifyNoInteractions(manageAllocationsDueToEndService)
    verifyNoInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, FIX_STUCK_AUTO_SUSPENDED)
  }

  @Test
  fun `activate, deallocate allocation due to end and deallocate allocation due to expire operations triggered`() {
    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_ENDING, DEALLOCATE_EXPIRING, FIX_STUCK_AUTO_SUSPENDED)

    job.execute(withActivate = true, withDeallocateEnding = true, withDeallocateExpiring = true, withFixAutoSuspended = true)

    verify(manageAllocationsDueToEndService).endAllocationsDueToEnd()
    verify(deallocationService).allocations()
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verify(manageAllocationsDueToExpireService).deallocateAllocationsDueToExpire()
    verify(deallocationService).fixPrisonersIncorrectlyAutoSuspended()
    verifyNoMoreInteractions(deallocationService)
    verifyNoMoreInteractions(manageAllocationsDueToEndService)
    verifyNoMoreInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_ENDING, DEALLOCATE_EXPIRING, FIX_STUCK_AUTO_SUSPENDED)
  }

  @Test
  fun `activate, deallocate allocation due to end and deallocate allocation due to expire operations triggered with distributed DEALLOCATE_ENDING`() {
    whenever(featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_ENDING_ENABLED)).thenReturn(true)

    mockJobs(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_ENDING, DEALLOCATE_EXPIRING, FIX_STUCK_AUTO_SUSPENDED)

    ManageAllocationsJob(
      rolloutPrisonService,
      deallocationService,
      manageAllocationsDueToEndService,
      manageAllocationsDueToExpireService,
      safeJobRunner,
      featureSwitches,
    )
      .execute(withActivate = true, withDeallocateEnding = true, withDeallocateExpiring = true, withFixAutoSuspended = true)

    verify(safeJobRunner).runDistributedJob(DEALLOCATE_ENDING, manageAllocationsDueToEndService::sendAllocationsDueToEndEvents)
    verify(manageAllocationsDueToEndService).sendAllocationsDueToEndEvents(jobCaptor.capture())
    assertThat(jobCaptor.firstValue.jobType).isEqualTo(DEALLOCATE_ENDING)
    verify(deallocationService).allocations()
    verify(deallocationService).suspendAllocationsDueToBeSuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).suspendAllocationsDueToBeSuspended(MOORLAND_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(PENTONVILLE_PRISON_CODE)
    verify(deallocationService).unsuspendAllocationsDueToBeUnsuspended(MOORLAND_PRISON_CODE)
    verify(manageAllocationsDueToExpireService).deallocateAllocationsDueToExpire()
    verify(deallocationService).fixPrisonersIncorrectlyAutoSuspended()
    verifyNoMoreInteractions(deallocationService)
    verifyNoMoreInteractions(manageAllocationsDueToEndService)
    verifyNoMoreInteractions(manageAllocationsDueToExpireService)

    verifyJobsWithRetryCalled(ALLOCATE, START_SUSPENSIONS, END_SUSPENSIONS, DEALLOCATE_EXPIRING, FIX_STUCK_AUTO_SUSPENDED)
  }
}

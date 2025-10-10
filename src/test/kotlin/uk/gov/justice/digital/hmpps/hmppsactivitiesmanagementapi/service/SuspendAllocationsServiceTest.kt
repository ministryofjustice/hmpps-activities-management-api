package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.END_SUSPENSIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.START_SUSPENSIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsTestBase
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.PrisonCodeJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesServiceTest.Companion.rolledOutPrisons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService

class SuspendAllocationsServiceTest : JobsTestBase() {
  private val rolloutPrisonService: RolloutPrisonService = mock {
    on { getRolloutPrisons() } doReturn listOf(rolloutPrison(PENTONVILLE_PRISON_CODE), rolloutPrison(MOORLAND_PRISON_CODE))
  }
  private val jobsSqsService: JobsSqsService = mock()
  private val jobService: JobService = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val monitoringService: MonitoringService = mock()
  private val unsuspendAllocationsService: UnsuspendAllocationsService = mock()
  private val nextJobCaptor = argumentCaptor<Job>()

  private val service =
    SuspendAllocationsService(
      rolloutPrisonService,
      TransactionHandler(),
      jobsSqsService,
      jobService,
      allocationRepository,
      safeJobRunner,
      unsuspendAllocationsService,
      outboundEventsService,
      monitoringService,
    )

  @BeforeEach
  fun setup() {
    whenever(rolloutPrisonService.isActivitiesRolledOutAt(PENTONVILLE_PRISON_CODE)) doReturn true
    whenever(rolloutPrisonService.isActivitiesRolledOutAt(MOORLAND_PRISON_CODE)) doReturn true
  }

  @Test
  fun `active allocations with a suspension due to start today are suspended`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it)
    }

    val activeAllocation: Allocation = allocation(withPlannedSuspensions = true)

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.ACTIVE)),
    ) doReturn listOf(activeAllocation)

    activeAllocation.prisonerStatus isEqualTo PrisonerStatus.ACTIVE

    service.suspendAllocationsDueToBeSuspended()

    activeAllocation.prisonerStatus isEqualTo PrisonerStatus.SUSPENDED

    verify(allocationRepository).saveAndFlush(activeAllocation)
  }

  @Test
  fun `should capture failures in monitoring service for any exceptions when suspending`() {
    val exception = RuntimeException("Something went wrong")
    doThrow(exception).whenever(allocationRepository).saveAndFlush(any())
    val prison = rolloutPrison().also { whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it) }
    val activeAllocation: Allocation = allocation(withPlannedSuspensions = true)
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.ACTIVE))) doReturn listOf(activeAllocation)

    service.suspendAllocationsDueToBeSuspended()

    verify(monitoringService).capture("An error occurred while suspending allocations due to be suspended today", exception)
  }

  @Test
  fun `handleEvent - active allocations with a suspension due to start today are suspended`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it)
    }

    val activeAllocation: Allocation = allocation(withPlannedSuspensions = true)

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.ACTIVE)),
    ) doReturn listOf(activeAllocation)

    activeAllocation.prisonerStatus isEqualTo PrisonerStatus.ACTIVE

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    activeAllocation.prisonerStatus isEqualTo PrisonerStatus.SUSPENDED

    verify(allocationRepository).saveAndFlush(activeAllocation)
    verify(jobService).incrementCount(123)
    verifyNoInteractions(safeJobRunner)
  }

  @Test
  fun `handleEvent - when all prisons are complete then end suspensions job is run`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it)
    }

    val activeAllocation: Allocation = allocation(withPlannedSuspensions = true)

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.ACTIVE)),
    ) doReturn listOf(activeAllocation)

    activeAllocation.prisonerStatus isEqualTo PrisonerStatus.ACTIVE

    mockJobs(END_SUSPENSIONS)

    whenever(jobService.incrementCount(123)).thenReturn(true)

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    activeAllocation.prisonerStatus isEqualTo PrisonerStatus.SUSPENDED

    verify(allocationRepository).saveAndFlush(activeAllocation)
    verify(jobService).incrementCount(123)

    verify(safeJobRunner).runDistributedJob(END_SUSPENSIONS, unsuspendAllocationsService::sendEvents)
    verify(unsuspendAllocationsService).sendEvents(nextJobCaptor.capture())
    assertThat(nextJobCaptor.firstValue.jobType).isEqualTo(END_SUSPENSIONS)
  }

  @Test
  fun `handleEvent - should capture failures in monitoring service for any exceptions when suspending`() {
    val exception = RuntimeException("Something went wrong")
    doThrow(exception).whenever(allocationRepository).saveAndFlush(any())
    val prison = rolloutPrison().also { whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it) }
    val activeAllocation: Allocation = allocation(withPlannedSuspensions = true)
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.ACTIVE))) doReturn listOf(activeAllocation)

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    verify(monitoringService).capture("An error occurred while suspending allocations due to be suspended today", exception)
    verify(jobService).incrementCount(123)
    verifyNoInteractions(safeJobRunner)
  }

  @Test
  fun `should send events to queue for each prison`() {
    service.sendEvents(Job(123, START_SUSPENSIONS))

    verify(jobService).initialiseCounts(123, rolledOutPrisons.count { it.prisonLive })

    verify(jobsSqsService).sendJobEvent(JobEventMessage(123, START_SUSPENSIONS, PrisonCodeJobEvent(PENTONVILLE_PRISON_CODE)))
    verify(jobsSqsService).sendJobEvent(JobEventMessage(123, START_SUSPENSIONS, PrisonCodeJobEvent(MOORLAND_PRISON_CODE)))

    verifyNoInteractions(safeJobRunner)
  }

  @Test
  fun `handleEvent - throws an exception if prison is not rolled out`() {
    assertThatThrownBy {
      service.handleEvent(123, RISLEY_PRISON_CODE)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Supplied prison $RISLEY_PRISON_CODE is not rolled out.")
  }
}

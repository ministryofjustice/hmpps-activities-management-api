package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.END_SUSPENSIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobEventMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.JobsSqsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.PrisonCodeJobEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesServiceTest.Companion.rolledOutPrisons
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.LocalDate

class UnsuspendAllocationsServiceTest {
  private val rolloutPrisonService: RolloutPrisonService = mock {
    on { getRolloutPrisons() } doReturn listOf(rolloutPrison(PENTONVILLE_PRISON_CODE), rolloutPrison(MOORLAND_PRISON_CODE))
  }
  private val jobsSqsService: JobsSqsService = mock()
  private val jobService: JobService = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val monitoringService: MonitoringService = mock()

  private val service =
    UnsuspendAllocationsService(
      rolloutPrisonService,
      TransactionHandler(),
      jobsSqsService,
      jobService,
      allocationRepository,
      outboundEventsService,
      monitoringService,
    )

  @BeforeEach
  fun setup() {
    whenever(rolloutPrisonService.isActivitiesRolledOutAt(PENTONVILLE_PRISON_CODE)) doReturn true
    whenever(rolloutPrisonService.isActivitiesRolledOutAt(MOORLAND_PRISON_CODE)) doReturn true
  }

  @Test
  fun `should capture failures in monitoring service for any exceptions when unsuspending`() {
    val exception = RuntimeException("Something went wrong")
    doThrow(exception).whenever(allocationRepository).saveAndFlush(any())
    val prison = rolloutPrison().also { whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it) }
    val suspendedAllocation: Allocation = allocation(withPlannedSuspensions = true).apply {
      activatePlannedSuspension()
      plannedSuspension()!!.endOn(LocalDate.now(), "TEST")
    }

    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.SUSPENDED, PrisonerStatus.SUSPENDED_WITH_PAY))) doReturn listOf(suspendedAllocation)

    service.unsuspendAllocationsDueToBeUnsuspended()

    verify(monitoringService).capture("An error occurred while unsuspending allocations due to be unsuspended today", exception)
  }

  @Test
  fun `suspended allocations with a suspension due to end today are activated`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it)
    }

    val suspendedAllocation: Allocation = allocation(withPlannedSuspensions = true).apply {
      activatePlannedSuspension()
      plannedSuspension()!!.endOn(LocalDate.now(), "TEST")
    }

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.SUSPENDED, PrisonerStatus.SUSPENDED_WITH_PAY)),
    ) doReturn listOf(suspendedAllocation)

    suspendedAllocation.prisonerStatus isEqualTo PrisonerStatus.SUSPENDED

    service.unsuspendAllocationsDueToBeUnsuspended()

    suspendedAllocation.prisonerStatus isEqualTo PrisonerStatus.ACTIVE

    verify(allocationRepository).saveAndFlush(suspendedAllocation)
  }

  @Test
  fun `suspended with pay allocations with a suspension due to end today are activated`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it)
    }

    val suspendedAllocation: Allocation = allocation(withPlannedSuspensions = true, withPaidSuspension = true).apply {
      activatePlannedSuspension()
      plannedSuspension()!!.endOn(LocalDate.now(), "TEST")
    }

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.SUSPENDED, PrisonerStatus.SUSPENDED_WITH_PAY)),
    ) doReturn listOf(suspendedAllocation)

    suspendedAllocation.prisonerStatus isEqualTo PrisonerStatus.SUSPENDED_WITH_PAY

    service.unsuspendAllocationsDueToBeUnsuspended()

    suspendedAllocation.prisonerStatus isEqualTo PrisonerStatus.ACTIVE

    verify(allocationRepository).saveAndFlush(suspendedAllocation)
  }

  @Test
  fun `handleEvent - should capture failures in monitoring service for any exceptions when unsuspending`() {
    val exception = RuntimeException("Something went wrong")
    doThrow(exception).whenever(allocationRepository).saveAndFlush(any())
    val prison = rolloutPrison().also { whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it) }
    val suspendedAllocation: Allocation = allocation(withPlannedSuspensions = true).apply {
      activatePlannedSuspension()
      plannedSuspension()!!.endOn(LocalDate.now(), "TEST")
    }

    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.SUSPENDED, PrisonerStatus.SUSPENDED_WITH_PAY))) doReturn listOf(suspendedAllocation)

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    verify(monitoringService).capture("An error occurred while unsuspending allocations due to be unsuspended today", exception)
    verify(jobService).incrementCount(123)
  }

  @Test
  fun `handleEvent - suspended allocations with a suspension due to end today are activated`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it)
    }

    val suspendedAllocation: Allocation = allocation(withPlannedSuspensions = true).apply {
      activatePlannedSuspension()
      plannedSuspension()!!.endOn(LocalDate.now(), "TEST")
    }

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.SUSPENDED, PrisonerStatus.SUSPENDED_WITH_PAY)),
    ) doReturn listOf(suspendedAllocation)

    suspendedAllocation.prisonerStatus isEqualTo PrisonerStatus.SUSPENDED

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    suspendedAllocation.prisonerStatus isEqualTo PrisonerStatus.ACTIVE

    verify(allocationRepository).saveAndFlush(suspendedAllocation)
    verify(jobService).incrementCount(123)
  }

  @Test
  fun `handleEvent - suspended with pay allocations with a suspension due to end today are activated`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it)
    }

    val suspendedAllocation: Allocation = allocation(withPlannedSuspensions = true, withPaidSuspension = true).apply {
      activatePlannedSuspension()
      plannedSuspension()!!.endOn(LocalDate.now(), "TEST")
    }

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.SUSPENDED, PrisonerStatus.SUSPENDED_WITH_PAY)),
    ) doReturn listOf(suspendedAllocation)

    suspendedAllocation.prisonerStatus isEqualTo PrisonerStatus.SUSPENDED_WITH_PAY

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    suspendedAllocation.prisonerStatus isEqualTo PrisonerStatus.ACTIVE

    verify(allocationRepository).saveAndFlush(suspendedAllocation)
    verify(jobService).incrementCount(123)
  }

  @Test
  fun `should send events to queue for each prison`() {
    service.sendEvents(Job(123, END_SUSPENSIONS))

    verify(jobService).initialiseCounts(123, rolledOutPrisons.count { it.prisonLive })

    verify(jobsSqsService).sendJobEvent(JobEventMessage(123, END_SUSPENSIONS, PrisonCodeJobEvent(PENTONVILLE_PRISON_CODE)))
    verify(jobsSqsService).sendJobEvent(JobEventMessage(123, END_SUSPENSIONS, PrisonCodeJobEvent(MOORLAND_PRISON_CODE)))
  }

  @Test
  fun `handleEvent - throws an exception if prison is not rolled out`() {
    assertThatThrownBy {
      service.handleEvent(123, RISLEY_PRISON_CODE)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Supplied prison $RISLEY_PRISON_CODE is not rolled out.")
  }
}

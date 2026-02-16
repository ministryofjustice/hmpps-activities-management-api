package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Job
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.START_SUSPENSIONS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
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

class ManageNewAllocationsServiceTest : JobsTestBase() {

  private val rolloutPrisonService: RolloutPrisonService = mock {
    on { getRolloutPrisons() } doReturn listOf(rolloutPrison(PENTONVILLE_PRISON_CODE), rolloutPrison(MOORLAND_PRISON_CODE))
  }
  private val allocationRepository: AllocationRepository = mock()
  private val prisonerSearch: PrisonerSearchApiClient = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val monitoringService: MonitoringService = mock()
  private val jobsSqsService: JobsSqsService = mock()
  private val jobService: JobService = mock()
  private val suspendAllocationsService: SuspendAllocationsService = mock()
  private val nextJobCaptor = argumentCaptor<Job>()

  private val service = ManageNewAllocationsService(
    rolloutPrisonService,
    TransactionHandler(),
    prisonerSearch,
    jobsSqsService,
    jobService,
    allocationRepository,
    safeJobRunner,
    suspendAllocationsService,
    outboundEventsService,
    monitoringService,
  )

  @BeforeEach
  fun setup() {
    whenever(rolloutPrisonService.isActivitiesRolledOutAt(PENTONVILLE_PRISON_CODE)) doReturn true
    whenever(rolloutPrisonService.isActivitiesRolledOutAt(MOORLAND_PRISON_CODE)) doReturn true
    whenever(prisonerSearch.findByPrisonerNumbers(any(), any())) doReturn emptyList()
  }

  @Test
  fun `should send events to queue for each prison`() {
    service.sendAllocationEvents(Job(123, JobType.ALLOCATE))

    verify(jobService).initialiseCounts(123, rolledOutPrisons.count { it.prisonLive })

    verify(jobsSqsService).sendJobEvent(JobEventMessage(123, JobType.ALLOCATE, PrisonCodeJobEvent(PENTONVILLE_PRISON_CODE)))
    verify(jobsSqsService).sendJobEvent(JobEventMessage(123, JobType.ALLOCATE, PrisonCodeJobEvent(MOORLAND_PRISON_CODE)))

    verifyNoInteractions(safeJobRunner)
  }

  @Test
  fun `pending allocations on or before today are auto-suspended when prisoner is out of prison`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it)
    }

    val pendingAllocationYesterday: Allocation = allocation().copy(
      allocationId = 1,
      prisonerNumber = "1",
      startDate = TimeSource.yesterday(),
      prisonerStatus = PrisonerStatus.PENDING,
    )

    val pendingAllocationToday: Allocation = allocation().copy(
      allocationId = 2,
      prisonerNumber = "2",
      startDate = TimeSource.today(),
      prisonerStatus = PrisonerStatus.PENDING,
    )

    whenever(prisonerSearch.findByPrisonerNumbers(listOf("1", "2"))) doReturn listOf(prisoner(pendingAllocationYesterday, "ACTIVE TRN"), prisoner(pendingAllocationToday, "ACTIVE IN", "PVI"))

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
        prison.prisonCode,
        PrisonerStatus.PENDING,
        TimeSource.today(),
      ),
    ) doReturn listOf(pendingAllocationYesterday, pendingAllocationToday)

    pendingAllocationYesterday.prisonerStatus isEqualTo PrisonerStatus.PENDING
    pendingAllocationToday.prisonerStatus isEqualTo PrisonerStatus.PENDING

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    listOf(pendingAllocationYesterday, pendingAllocationToday).forEach { allocation ->
      allocation.prisonerStatus isEqualTo PrisonerStatus.AUTO_SUSPENDED
      allocation.suspendedReason isEqualTo "Temporarily released or transferred"
      allocation.suspendedTime isCloseTo TimeSource.now()
    }

    verify(allocationRepository).saveAndFlush(pendingAllocationYesterday)
    verify(allocationRepository).saveAndFlush(pendingAllocationToday)
    verify(jobService).incrementCount(123)

    verifyNoInteractions(safeJobRunner)
  }

  @Test
  fun `when all prisons are complete then start suspensions job is run`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it)
    }

    val allocation: Allocation = allocation().copy(
      allocationId = 1,
      prisonerNumber = "1",
      startDate = TimeSource.yesterday(),
      prisonerStatus = PrisonerStatus.PENDING,
    )

    whenever(prisonerSearch.findByPrisonerNumbers(listOf("1"))) doReturn listOf(prisoner(allocation, "ACTIVE TRN"))

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
        prison.prisonCode,
        PrisonerStatus.PENDING,
        TimeSource.today(),
      ),
    ) doReturn listOf(allocation)

    mockJobs(START_SUSPENSIONS)

    whenever(jobService.incrementCount(123)).thenReturn(true)

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    allocation.prisonerStatus isEqualTo PrisonerStatus.AUTO_SUSPENDED
    allocation.suspendedReason isEqualTo "Temporarily released or transferred"
    allocation.suspendedTime isCloseTo TimeSource.now()

    verify(allocationRepository).saveAndFlush(allocation)
    verify(jobService).incrementCount(123)

    verify(safeJobRunner).runDistributedJob(START_SUSPENSIONS, suspendAllocationsService::sendEvents)
    verify(suspendAllocationsService).sendEvents(nextJobCaptor.capture())
    assertThat(nextJobCaptor.firstValue.jobType).isEqualTo(START_SUSPENSIONS)
  }

  @Test
  fun `pending allocation not processed when prisoner not found`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it)
    }

    val pendingAllocation: Allocation = allocation().copy(
      allocationId = 1,
      prisonerNumber = "1",
      startDate = TimeSource.yesterday(),
      prisonerStatus = PrisonerStatus.PENDING,
    )

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
        prison.prisonCode,
        PrisonerStatus.PENDING,
        TimeSource.today(),
      ),
    ) doReturn listOf(pendingAllocation)

    pendingAllocation.prisonerStatus isEqualTo PrisonerStatus.PENDING

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    pendingAllocation.prisonerStatus isEqualTo PrisonerStatus.PENDING

    verify(allocationRepository, never()).saveAndFlush(pendingAllocation)

    verifyNoInteractions(safeJobRunner)
  }

  @Test
  fun `pending allocation is activated`() {
    val prison = rolloutPrison().also {
      whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(it)
    }

    val pendingAllocation: Allocation = allocation().copy(
      allocationId = 1,
      prisonerNumber = "1",
      startDate = TimeSource.yesterday(),
      prisonerStatus = PrisonerStatus.PENDING,
    )

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
        prison.prisonCode,
        PrisonerStatus.PENDING,
        TimeSource.today(),
      ),
    ) doReturn listOf(pendingAllocation)

    whenever(prisonerSearch.findByPrisonerNumbers(listOf("1"))) doReturn listOf(prisoner(pendingAllocation, "ACTIVE IN"))

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
        prison.prisonCode,
        PrisonerStatus.PENDING,
        TimeSource.today(),
      ),
    ) doReturn listOf(pendingAllocation)

    pendingAllocation.prisonerStatus isEqualTo PrisonerStatus.PENDING

    service.handleEvent(123, PENTONVILLE_PRISON_CODE)

    pendingAllocation.prisonerStatus isEqualTo PrisonerStatus.ACTIVE

    verify(allocationRepository).saveAndFlush(pendingAllocation)

    verifyNoInteractions(safeJobRunner)
  }

  @Test
  fun `throws an exception if prison is not rolled out`() {
    assertThatThrownBy {
      service.handleEvent(123, RISLEY_PRISON_CODE)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Supplied prison $RISLEY_PRISON_CODE is not rolled out.")
  }

  private fun prisoner(allocation: Allocation, status: String, prisonCode: String = "MDI"): Prisoner = PrisonerSearchPrisonerFixture.instance(prisonerNumber = allocation.prisonerNumber, status = status, prisonId = prisonCode)
}

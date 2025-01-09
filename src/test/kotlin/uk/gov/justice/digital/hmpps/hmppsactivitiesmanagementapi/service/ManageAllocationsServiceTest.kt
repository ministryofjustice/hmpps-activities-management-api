package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.LocalDate

class ManageAllocationsServiceTest {

  private val rolloutPrisonService: RolloutPrisonService = mock()
  private val activityScheduleRepo: ActivityScheduleRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val searchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val waitingListService: WaitingListService = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val monitoringService: MonitoringService = mock()

  private val service =
    ManageAllocationsService(
      rolloutPrisonService,
      activityScheduleRepo,
      allocationRepository,
      searchApiClient,
      waitingListService,
      TransactionHandler(),
      outboundEventsService,
      prisonApiClient,
      monitoringService,
    )
  private val yesterday = LocalDate.now().minusDays(1)
  private val today = yesterday.plusDays(1)

  @BeforeEach
  fun setup() {
    whenever(searchApiClient.findByPrisonerNumbers(any(), any())) doReturn emptyList()
  }

  @Test
  fun `deallocate offenders from activity ending today without pending deallocation`() {
    val prison = rolloutPrison()
    val schedule = activitySchedule(activityEntity(startDate = yesterday, endDate = today))
    val allocation = schedule.allocations().first().also { it.verifyIsActive() }

    whenever(rolloutPrisonService.isActivitiesRolledOutAt(prison.prisonCode)) doReturn true
    whenever(activityScheduleRepo.findAllByActivityPrisonCode(prison.prisonCode)) doReturn listOf(schedule)

    service.endAllocationsDueToEnd(prison.prisonCode, LocalDate.now())

    allocation.verifyIsEnded()

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `deallocate offenders from activity ending today declines pending or approved waiting lists`() {
    val prison = rolloutPrison()
    val schedule = activitySchedule(activityEntity(startDate = yesterday, endDate = today))

    whenever(rolloutPrisonService.isActivitiesRolledOutAt(prison.prisonCode)) doReturn true
    whenever(activityScheduleRepo.findAllByActivityPrisonCode(prison.prisonCode)) doReturn listOf(schedule)

    service.endAllocationsDueToEnd(prison.prisonCode, LocalDate.now())

    verify(waitingListService).declinePendingOrApprovedApplications(
      schedule.activity.activityId,
      "Activity ended",
      "Activities Management Service",
    )
  }

  @Test
  fun `deallocate offenders from activity ending today with pending deallocation`() {
    val prison = rolloutPrison()
    val schedule = activitySchedule(activityEntity(startDate = yesterday, endDate = today))
    val allocation = schedule.allocations().first().also { it.verifyIsActive() }
    allocation.deallocateOn(today, DeallocationReason.OTHER, "by test")

    whenever(rolloutPrisonService.isActivitiesRolledOutAt(prison.prisonCode)) doReturn true
    whenever(activityScheduleRepo.findAllByActivityPrisonCode(prison.prisonCode)) doReturn listOf(schedule)

    service.endAllocationsDueToEnd(prison.prisonCode, LocalDate.now())

    allocation.verifyIsEnded(DeallocationReason.OTHER, "by test")

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `deallocate offenders from activity with no end date and allocation ends today`() {
    val prison = rolloutPrison()
    val schedule = activitySchedule(activityEntity(startDate = yesterday, endDate = null))
    val allocation = schedule.allocations().first().apply { endDate = today }.also { it.verifyIsActive() }

    whenever(rolloutPrisonService.isActivitiesRolledOutAt(prison.prisonCode)) doReturn true
    whenever(activityScheduleRepo.findAllByActivityPrisonCode(prison.prisonCode)) doReturn listOf(schedule)

    service.endAllocationsDueToEnd(prison.prisonCode, LocalDate.now())

    allocation.verifyIsEnded()

    verify(activityScheduleRepo).saveAndFlush(schedule)
    verifyNoInteractions(waitingListService)
  }

  @Test
  fun `offenders not deallocated from activity with no end date and allocation does not end today`() {
    val prison = rolloutPrison()
    val schedule = activitySchedule(activityEntity(startDate = yesterday, endDate = null))
    val allocation = schedule.allocations().first().also { it.verifyIsActive() }

    whenever(rolloutPrisonService.isActivitiesRolledOutAt(prison.prisonCode)) doReturn true
    whenever(activityScheduleRepo.findAllByActivityPrisonCode(prison.prisonCode)) doReturn listOf(schedule)

    service.endAllocationsDueToEnd(prison.prisonCode, LocalDate.now())

    allocation.verifyIsActive()

    verify(activityScheduleRepo, never()).saveAndFlush(any())
    verifyNoInteractions(waitingListService)
  }

  @Test
  fun `prisoners are deallocated from allocations pending due to expire`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.OUT
      on { prisonerNumber } doReturn allocation.prisonerNumber
    }

    whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(prison)
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.PENDING))) doReturn listOf(
      allocation,
    )
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))) doReturn listOf(prisoner)

    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf(allocation.prisonerNumber))) doReturn
      listOf(movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()))

    service.allocations(AllocationOperation.EXPIRING_TODAY)

    allocation.verifyIsExpired()

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `should capture failures in monitoring service for any exceptions when expiring`() {
    val exception = RuntimeException("Something went wrong")
    doThrow(exception).whenever(activityScheduleRepo).saveAndFlush(any())
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.OUT
      on { prisonerNumber } doReturn allocation.prisonerNumber
    }

    whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(prison)
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.PENDING))) doReturn listOf(allocation)
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))) doReturn listOf(prisoner)
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf(allocation.prisonerNumber))) doReturn listOf(movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()))

    service.allocations(AllocationOperation.EXPIRING_TODAY)

    verify(monitoringService).capture("An error occurred deallocating allocations on activity schedule 1", exception)
  }

  @Test
  fun `prisoners are deallocated from allocations pending due to expire when at a different prison`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }
    val prisonerInAtOtherPrison: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.IN
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { prisonId } doReturn prison.prisonCode.plus("-other")
    }

    whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(prison)
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.PENDING))) doReturn listOf(
      allocation,
    )
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisonerInAtOtherPrison.prisonerNumber))) doReturn listOf(prisonerInAtOtherPrison)

    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf(allocation.prisonerNumber))) doReturn
      listOf(movement(prisonerNumber = allocation.prisonerNumber, fromPrisonCode = prison.prisonCode, movementDate = TimeSource.yesterday()))

    service.allocations(AllocationOperation.EXPIRING_TODAY)

    allocation.verifyIsExpired()

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `prisoners are are not deallocated from allocations pending as not due to expire`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = TimeSource.tomorrow())
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.prisonerStatus isEqualTo PrisonerStatus.PENDING }
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.OUT
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { prisonId } doReturn prison.prisonCode
    }

    whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(prison)
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.prisonCode, listOf(PrisonerStatus.PENDING))) doReturn listOf(
      allocation,
    )
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))) doReturn listOf(prisoner)

    // Multiple moves to demonstrate takes the latest move for an offender
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf(allocation.prisonerNumber))) doReturn
      listOf(
        movement(prisonerNumber = allocation.prisonerNumber, movementDate = TimeSource.yesterday()),
        movement(prisonerNumber = allocation.prisonerNumber, fromPrisonCode = prison.prisonCode, movementDate = TimeSource.today()),
      )

    service.allocations(AllocationOperation.EXPIRING_TODAY)

    verify(waitingListService, never()).removeOpenApplications(any(), any(), any())

    allocation.prisonerStatus isEqualTo PrisonerStatus.PENDING

    verify(activityScheduleRepo, never()).saveAndFlush(schedule)
  }

  @Test
  fun `prisoners due to expire waiting lists are removed`() {
    val prison = rolloutPrison()
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.OUT
      on { prisonerNumber } doReturn "A1234AA"
    }

    whenever(rolloutPrisonService.getRolloutPrisons()) doReturn listOf(prison)
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))) doReturn listOf(prisoner)
    whenever(waitingListService.fetchOpenApplicationsForPrison(prison.prisonCode)) doReturn listOf(waitingList(prisonerNumber = "A1234AA"))
    whenever(prisonApiClient.getMovementsForPrisonersFromPrison(prison.prisonCode, setOf("A1234AA"))) doReturn
      listOf(movement(prisonerNumber = "A1234AA", movementDate = TimeSource.yesterday()))

    service.allocations(AllocationOperation.EXPIRING_TODAY)

    verify(waitingListService).removeOpenApplications(
      prison.prisonCode,
      "A1234AA",
      ServiceName.SERVICE_NAME.value,
    )
  }

  @Test
  fun `pending allocations on or before today are correctly activated`() {
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

    whenever(searchApiClient.findByPrisonerNumbers(listOf("1", "2"))) doReturn listOf(prisoner(pendingAllocationYesterday, "ACTIVE IN"), prisoner(pendingAllocationToday, "ACTIVE IN"))

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
        prison.prisonCode,
        PrisonerStatus.PENDING,
        TimeSource.today(),
      ),
    ) doReturn listOf(pendingAllocationYesterday, pendingAllocationToday)

    pendingAllocationYesterday.prisonerStatus isEqualTo PrisonerStatus.PENDING
    pendingAllocationToday.prisonerStatus isEqualTo PrisonerStatus.PENDING

    service.allocations(AllocationOperation.STARTING_TODAY)

    pendingAllocationYesterday.prisonerStatus isEqualTo PrisonerStatus.ACTIVE
    pendingAllocationToday.prisonerStatus isEqualTo PrisonerStatus.ACTIVE

    verify(allocationRepository).saveAndFlush(pendingAllocationYesterday)
    verify(allocationRepository).saveAndFlush(pendingAllocationToday)
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

    whenever(searchApiClient.findByPrisonerNumbers(listOf("1", "2"))) doReturn listOf(prisoner(pendingAllocationYesterday, "ACTIVE TRN"), prisoner(pendingAllocationToday, "ACTIVE IN", "PVI"))

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
        prison.prisonCode,
        PrisonerStatus.PENDING,
        TimeSource.today(),
      ),
    ) doReturn listOf(pendingAllocationYesterday, pendingAllocationToday)

    pendingAllocationYesterday.prisonerStatus isEqualTo PrisonerStatus.PENDING
    pendingAllocationToday.prisonerStatus isEqualTo PrisonerStatus.PENDING

    service.allocations(AllocationOperation.STARTING_TODAY)

    listOf(pendingAllocationYesterday, pendingAllocationToday).forEach { allocation ->
      allocation.prisonerStatus isEqualTo PrisonerStatus.AUTO_SUSPENDED
      allocation.suspendedReason isEqualTo "Temporarily released or transferred"
      allocation.suspendedTime isCloseTo TimeSource.now()
    }

    verify(allocationRepository).saveAndFlush(pendingAllocationYesterday)
    verify(allocationRepository).saveAndFlush(pendingAllocationToday)
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
    ).also {
      whenever(searchApiClient.findByPrisonerNumber(it.prisonerNumber)) doReturn null
    }

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
        prison.prisonCode,
        PrisonerStatus.PENDING,
        TimeSource.today(),
      ),
    ) doReturn listOf(pendingAllocation)

    pendingAllocation.prisonerStatus isEqualTo PrisonerStatus.PENDING

    service.allocations(AllocationOperation.STARTING_TODAY)

    pendingAllocation.prisonerStatus isEqualTo PrisonerStatus.PENDING

    verify(allocationRepository, never()).saveAndFlush(pendingAllocation)
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

    service.suspendAllocationsDueToBeSuspended(prison.prisonCode)

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

    service.suspendAllocationsDueToBeSuspended(prison.prisonCode)

    verify(monitoringService).capture("An error occurred while suspending allocations due to be suspended today", exception)
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

    service.unsuspendAllocationsDueToBeUnsuspended(prison.prisonCode)
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

    service.unsuspendAllocationsDueToBeUnsuspended(prison.prisonCode)

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

    service.unsuspendAllocationsDueToBeUnsuspended(prison.prisonCode)

    suspendedAllocation.prisonerStatus isEqualTo PrisonerStatus.ACTIVE

    verify(allocationRepository).saveAndFlush(suspendedAllocation)
  }

  private fun prisoner(allocation: Allocation, status: String, prisonCode: String = "MDI"): Prisoner =
    PrisonerSearchPrisonerFixture.instance(prisonerNumber = allocation.prisonerNumber, status = status, prisonId = prisonCode)

  private fun Allocation.verifyIsActive() {
    prisonerStatus isEqualTo PrisonerStatus.ACTIVE
  }

  private fun Allocation.verifyIsEnded(
    reason: DeallocationReason = DeallocationReason.ENDED,
    endedBy: String? = "Activities Management Service",
  ) {
    prisonerStatus isEqualTo PrisonerStatus.ENDED
    deallocatedTime!! isCloseTo TimeSource.now()
    deallocatedReason isEqualTo reason
    deallocatedBy isEqualTo endedBy
  }

  private fun Allocation.verifyIsExpired() {
    prisonerStatus isEqualTo PrisonerStatus.ENDED
    deallocatedTime!! isCloseTo TimeSource.now()
    deallocatedReason isEqualTo DeallocationReason.TEMPORARILY_RELEASED
    deallocatedBy isEqualTo "Activities Management Service"
  }
}

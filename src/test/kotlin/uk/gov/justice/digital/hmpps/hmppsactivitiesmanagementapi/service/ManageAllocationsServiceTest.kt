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
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.LocalDate

class ManageAllocationsServiceTest {

  private val rolloutPrisonService: RolloutPrisonService = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val prisonerSearch: PrisonerSearchApiApplicationClient = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val monitoringService: MonitoringService = mock()
  private val prisonerSearchApiApplicationClient: PrisonerSearchApiApplicationClient = mock()
  private val prisonerReceivedHandler: PrisonerReceivedHandler = mock()

  private val service =
    ManageAllocationsService(
      rolloutPrisonService,
      allocationRepository,
      prisonerSearch,
      TransactionHandler(),
      outboundEventsService,
      monitoringService,
      prisonerSearchApiApplicationClient,
      prisonerReceivedHandler,
    )

  @BeforeEach
  fun setup() {
    whenever(prisonerSearch.findByPrisonerNumbers(any(), any())) doReturn emptyList()
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

    service.allocations()

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
      whenever(prisonerSearch.findByPrisonerNumber(it.prisonerNumber)) doReturn null
    }

    whenever(
      allocationRepository.findByPrisonCodePrisonerStatusStartingOnOrBeforeDate(
        prison.prisonCode,
        PrisonerStatus.PENDING,
        TimeSource.today(),
      ),
    ) doReturn listOf(pendingAllocation)

    pendingAllocation.prisonerStatus isEqualTo PrisonerStatus.PENDING

    service.allocations()

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

  @Test
  fun `fix auto-suspended allocations for prisoner who should not be auto suspended`() {
    val allocation = allocation().copy(prisonerNumber = "A1111AA")

    whenever(allocationRepository.findActiveAllocations(PrisonerStatus.AUTO_SUSPENDED)).thenReturn(listOf(allocation))

    val prisoner: Prisoner = mock {
      on { prisonId } doReturn allocation.activitySchedule.activity.prisonCode
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { status } doReturn "ACTIVE IN"
    }

    whenever(prisonerSearchApiApplicationClient.findByPrisonerNumber(allocation.prisonerNumber)) doReturn prisoner

    service.fixPrisonersIncorrectlyAutoSuspended()

    verify(prisonerReceivedHandler).receivePrisoner(allocation.prisonCode(), allocation.prisonerNumber)
    verifyNoMoreInteractions(prisonerReceivedHandler)
  }

  @Test
  fun `fix auto-suspended allocations for prisoner who has multiple allocations in same prison`() {
    val allocation1 = allocation().copy(prisonerNumber = "A1111AA")
    val allocation2 = allocation().copy(prisonerNumber = "A1111AA")

    whenever(allocationRepository.findActiveAllocations(PrisonerStatus.AUTO_SUSPENDED)).thenReturn(listOf(allocation1, allocation2))

    val prisoner: Prisoner = mock {
      on { prisonId } doReturn allocation1.activitySchedule.activity.prisonCode
      on { prisonerNumber } doReturn allocation1.prisonerNumber
      on { status } doReturn "ACTIVE IN"
    }

    whenever(prisonerSearchApiApplicationClient.findByPrisonerNumber(allocation1.prisonerNumber)) doReturn prisoner

    service.fixPrisonersIncorrectlyAutoSuspended()

    verify(prisonerReceivedHandler).receivePrisoner(allocation1.prisonCode(), allocation1.prisonerNumber)
    verifyNoMoreInteractions(prisonerReceivedHandler)
  }

  @Test
  fun `fix auto-suspended allocations for prisoner who has multiple allocations in different prisons`() {
    val allocation1 = allocation().copy(activitySchedule = activitySchedule(activityEntity(prisonCode = "PVI"), noExclusions = true), prisonerNumber = "A1111AA")
    val allocation2 = allocation().copy(prisonerNumber = "A1111AA")

    whenever(allocationRepository.findActiveAllocations(PrisonerStatus.AUTO_SUSPENDED)).thenReturn(listOf(allocation1, allocation2))

    val prisoner: Prisoner = mock {
      on { prisonId } doReturn allocation2.activitySchedule.activity.prisonCode
      on { prisonerNumber } doReturn allocation2.prisonerNumber
      on { status } doReturn "ACTIVE IN"
    }

    whenever(prisonerSearchApiApplicationClient.findByPrisonerNumber(allocation2.prisonerNumber)) doReturn prisoner

    service.fixPrisonersIncorrectlyAutoSuspended()

    verify(prisonerReceivedHandler).receivePrisoner(allocation2.prisonCode(), allocation2.prisonerNumber)
    verifyNoMoreInteractions(prisonerReceivedHandler)
  }

  @Test
  fun `fix auto-suspended allocations for multiple prisoners`() {
    val allocation1 = allocation().copy(prisonerNumber = "A1111AA")
    val allocation2 = allocation().copy(activitySchedule = activitySchedule(activityEntity(prisonCode = "PVI"), noExclusions = true), prisonerNumber = "B1111BB")

    whenever(allocationRepository.findActiveAllocations(PrisonerStatus.AUTO_SUSPENDED)).thenReturn(listOf(allocation1, allocation2))

    val prisoner1: Prisoner = mock {
      on { prisonId } doReturn allocation1.activitySchedule.activity.prisonCode
      on { prisonerNumber } doReturn allocation1.prisonerNumber
      on { status } doReturn "ACTIVE IN"
    }

    val prisoner2: Prisoner = mock {
      on { prisonId } doReturn allocation2.activitySchedule.activity.prisonCode
      on { prisonerNumber } doReturn allocation2.prisonerNumber
      on { status } doReturn "ACTIVE IN"
    }

    whenever(prisonerSearchApiApplicationClient.findByPrisonerNumber(allocation1.prisonerNumber)) doReturn prisoner1
    whenever(prisonerSearchApiApplicationClient.findByPrisonerNumber(allocation2.prisonerNumber)) doReturn prisoner2

    service.fixPrisonersIncorrectlyAutoSuspended()

    verify(prisonerReceivedHandler).receivePrisoner(allocation1.prisonCode(), allocation1.prisonerNumber)
    verify(prisonerReceivedHandler).receivePrisoner(allocation2.prisonCode(), allocation2.prisonerNumber)
    verifyNoMoreInteractions(prisonerReceivedHandler)
  }

  @Test
  fun `only auto-suspended fix allocations for prisoners who are active in prison`() {
    val allocation = allocation().copy(prisonerNumber = "A1111AA")

    whenever(allocationRepository.findActiveAllocations(PrisonerStatus.AUTO_SUSPENDED)).thenReturn(listOf(allocation))

    val prisoner: Prisoner = mock {
      on { prisonId } doReturn allocation.activitySchedule.activity.prisonCode
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { status } doReturn "ACTIVE OUT"
    }

    whenever(prisonerSearchApiApplicationClient.findByPrisonerNumber(allocation.prisonerNumber)) doReturn prisoner

    service.fixPrisonersIncorrectlyAutoSuspended()

    verifyNoInteractions(prisonerReceivedHandler)
  }

  @Test
  fun `will continue to fix auto-suspended allocations after a previous prisoner is not found by search api`() {
    val allocation1 = allocation().copy(prisonerNumber = "A1111AA")
    val allocation2 = allocation().copy(activitySchedule = activitySchedule(activityEntity(prisonCode = "PVI"), noExclusions = true), prisonerNumber = "B1111BB")

    whenever(allocationRepository.findActiveAllocations(PrisonerStatus.AUTO_SUSPENDED)).thenReturn(listOf(allocation1, allocation2))

    val prisoner2: Prisoner = mock {
      on { prisonId } doReturn allocation2.activitySchedule.activity.prisonCode
      on { prisonerNumber } doReturn allocation2.prisonerNumber
      on { status } doReturn "ACTIVE IN"
    }

    whenever(prisonerSearchApiApplicationClient.findByPrisonerNumber(allocation1.prisonerNumber)) doReturn null
    whenever(prisonerSearchApiApplicationClient.findByPrisonerNumber(allocation2.prisonerNumber)) doReturn prisoner2

    service.fixPrisonersIncorrectlyAutoSuspended()

    verify(prisonerReceivedHandler).receivePrisoner(allocation2.prisonCode(), allocation2.prisonerNumber)
    verifyNoMoreInteractions(prisonerReceivedHandler)
  }

  private fun prisoner(allocation: Allocation, status: String, prisonCode: String = "MDI"): Prisoner = PrisonerSearchPrisonerFixture.instance(prisonerNumber = allocation.prisonerNumber, status = status, prisonId = prisonCode)
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

class ManageAllocationsServiceTest {

  private val allocationRepository: AllocationRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val monitoringService: MonitoringService = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val prisonerReceivedHandler: PrisonerReceivedHandler = mock()

  private val service =
    ManageAllocationsService(
      allocationRepository,
      outboundEventsService,
      monitoringService,
      prisonerSearchApiClient,
      prisonerReceivedHandler,
    )

  @Test
  fun `fix auto-suspended allocations for prisoner who should not be auto suspended`() {
    val allocation = allocation().copy(prisonerNumber = "A1111AA")

    whenever(allocationRepository.findActiveAllocations(PrisonerStatus.AUTO_SUSPENDED)).thenReturn(listOf(allocation))

    val prisoner: Prisoner = mock {
      on { prisonId } doReturn allocation.activitySchedule.activity.prisonCode
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { status } doReturn "ACTIVE IN"
    }

    whenever(prisonerSearchApiClient.findByPrisonerNumber(allocation.prisonerNumber)) doReturn prisoner

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

    whenever(prisonerSearchApiClient.findByPrisonerNumber(allocation1.prisonerNumber)) doReturn prisoner

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

    whenever(prisonerSearchApiClient.findByPrisonerNumber(allocation2.prisonerNumber)) doReturn prisoner

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

    whenever(prisonerSearchApiClient.findByPrisonerNumber(allocation1.prisonerNumber)) doReturn prisoner1
    whenever(prisonerSearchApiClient.findByPrisonerNumber(allocation2.prisonerNumber)) doReturn prisoner2

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

    whenever(prisonerSearchApiClient.findByPrisonerNumber(allocation.prisonerNumber)) doReturn prisoner

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

    whenever(prisonerSearchApiClient.findByPrisonerNumber(allocation1.prisonerNumber)) doReturn null
    whenever(prisonerSearchApiClient.findByPrisonerNumber(allocation2.prisonerNumber)) doReturn prisoner2

    service.fixPrisonersIncorrectlyAutoSuspended()

    verify(prisonerReceivedHandler).receivePrisoner(allocation2.prisonCode(), allocation2.prisonerNumber)
    verifyNoMoreInteractions(prisonerReceivedHandler)
  }
}

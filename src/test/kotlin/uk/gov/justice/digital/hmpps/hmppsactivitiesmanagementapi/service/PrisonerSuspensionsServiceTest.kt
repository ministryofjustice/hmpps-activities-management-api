package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteSubType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PlannedSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AddCaseNoteRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.SuspendPrisonerRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.UnsuspendPrisonerRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.time.LocalDate
import java.time.LocalDateTime

class PrisonerSuspensionsServiceTest {
  private val caseLoad = MOORLAND_PRISON_CODE

  private val allocationRepository: AllocationRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val attendanceSuspensionDomainService: AttendanceSuspensionDomainService = mock()
  private val caseNotesApiClient: CaseNotesApiClient = mock<CaseNotesApiClient>().also {
    whenever(it.postCaseNote(any(), any(), any(), any(), any(), any())) doReturn CaseNote(
      caseNoteId = "10001",
      offenderIdentifier = "1",
      type = "NEG",
      typeDescription = "Negative Behaviour",
      subType = "NEG_GEN",
      subTypeDescription = "General Entry",
      source = "INST",
      creationDateTime = LocalDateTime.now(),
      occurrenceDateTime = LocalDateTime.now(),
      authorName = "Test",
      authorUserId = "1",
      text = "Test case note",
      eventId = 1,
      sensitive = false,
    )
  }

  private val service: PrisonerSuspensionsService = PrisonerSuspensionsService(allocationRepository, caseNotesApiClient, attendanceSuspensionDomainService, outboundEventsService, TransactionHandler())
  private val allocationCaptor = argumentCaptor<List<Allocation>>()

  @BeforeEach
  fun setUp() {
    openMocks(this)
    addCaseloadIdToRequestHeader(caseLoad)
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Test
  fun `suspension start date must be on or after todays date`() {
    val allocation = allocation()
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = LocalDate.now().minusDays(1),
      status = PrisonerStatus.SUSPENDED,
    )

    assertThatThrownBy {
      service.suspend(prisonCode, suspendPrisonerRequest, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Suspension start date must be on or after today's date")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `suspension start date must be on or before the end date of the allocation`() {
    val allocation = allocation().apply { endDate = LocalDate.now().plusWeeks(1) }
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = allocation.startDate.plusWeeks(2),
      status = PrisonerStatus.SUSPENDED,
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    assertThatThrownBy {
      service.suspend(prisonCode, suspendPrisonerRequest, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation 0: Suspension start date must be on or before the allocation end date ${allocation.endDate!!.toIsoDate()}")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `a new general case note is posted and associated with the suspension`() {
    val allocation = allocation()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = allocation.startDate.plusWeeks(1),
      suspensionCaseNote = AddCaseNoteRequest(type = CaseNoteType.GEN, text = "test case note"),
      status = PrisonerStatus.SUSPENDED,
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    service.suspend(prisonCode, suspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    assertThat(allocationCaptor.firstValue.first().plannedSuspension()).isNotNull
    with(allocationCaptor.firstValue.first().plannedSuspension()!!) {
      startDate() isEqualTo allocation.startDate.plusWeeks(1)
      caseNoteId() isEqualTo 10001
    }
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoMoreInteractions(outboundEventsService)

    verify(caseNotesApiClient).postCaseNote(
      prisonCode,
      allocation.prisonerNumber,
      "test case note",
      CaseNoteType.GEN,
      CaseNoteSubType.HIS,
      "Suspended from activity from ${allocation.startDate.plusWeeks(1).toMediumFormatStyle()} - schedule description",
    )
    verifyNoMoreInteractions(caseNotesApiClient)
  }

  @Test
  fun `a new negative case note is posted and associated with the suspension`() {
    val allocation = allocation()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = allocation.startDate.plusWeeks(1),
      suspensionCaseNote = AddCaseNoteRequest(type = CaseNoteType.NEG, text = "test case note"),
      status = PrisonerStatus.SUSPENDED,
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    service.suspend(prisonCode, suspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    assertThat(allocationCaptor.firstValue.first().plannedSuspension()).isNotNull
    with(allocationCaptor.firstValue.first().plannedSuspension()!!) {
      startDate() isEqualTo allocation.startDate.plusWeeks(1)
      caseNoteId() isEqualTo 10001
    }
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoMoreInteractions(outboundEventsService)

    verify(caseNotesApiClient).postCaseNote(
      prisonCode,
      allocation.prisonerNumber,
      "test case note",
      CaseNoteType.NEG,
      CaseNoteSubType.NEG_GEN,
      "Suspended from activity from ${allocation.startDate.plusWeeks(1).toMediumFormatStyle()} - schedule description",
    )
    verifyNoMoreInteractions(caseNotesApiClient)
  }

  @Test
  fun `a new general case note is posted and associated with multiple allocations`() {
    val allocation = allocation()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = allocation.startDate.plusWeeks(1),
      suspensionCaseNote = AddCaseNoteRequest(type = CaseNoteType.GEN, text = "test case note"),
      status = PrisonerStatus.SUSPENDED,
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation(), allocation()))

    service.suspend(prisonCode, suspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    assertThat(allocationCaptor.firstValue.first().plannedSuspension()).isNotNull
    with(allocationCaptor.firstValue.first().plannedSuspension()!!) {
      startDate() isEqualTo allocation.startDate.plusWeeks(1)
      caseNoteId() isEqualTo 10001
    }
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoMoreInteractions(outboundEventsService)

    verify(caseNotesApiClient).postCaseNote(
      prisonCode,
      allocation.prisonerNumber,
      "test case note",
      CaseNoteType.GEN,
      CaseNoteSubType.HIS,
      "Suspended from all activities from ${allocation.startDate.plusWeeks(1).toMediumFormatStyle()}",
    )
    verifyNoMoreInteractions(caseNotesApiClient)
  }

  @Test
  fun `a new planned suspension is created if one doesnt exist`() {
    val allocation = allocation()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = allocation.startDate.plusWeeks(1),
      status = PrisonerStatus.SUSPENDED,
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    service.suspend(prisonCode, suspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    assertThat(allocationCaptor.firstValue.first().plannedSuspension()).isNotNull
    with(allocationCaptor.firstValue.first().plannedSuspension()!!) {
      startDate() isEqualTo allocation.startDate.plusWeeks(1)
    }
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoMoreInteractions(outboundEventsService)
    verifyNoInteractions(caseNotesApiClient)
  }

  @Test
  fun `an existing planned suspension is updated if it hasnt started yet`() {
    val allocation = allocation().apply {
      addPlannedSuspension(
        PlannedSuspension(
          allocation = this,
          plannedStartDate = startDate.plusWeeks(1),
          plannedBy = "Test",
        ),
      )
    }

    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = allocation.startDate,
      status = PrisonerStatus.SUSPENDED,
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    val originalSuspension = allocation.plannedSuspension()!!
    originalSuspension.startDate() isEqualTo allocation.startDate.plusWeeks(1)

    service.suspend(prisonCode, suspendPrisonerRequest, "user")
    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    val newSuspension = allocationCaptor.firstValue.first().plannedSuspension()!!

    with(newSuspension) {
      newSuspension isEqualTo originalSuspension
      startDate() isEqualTo allocation.startDate
      endDate() isEqualTo null
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `suspension start date today immediately suspends the allocation`() {
    val allocation = allocation()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = allocation.startDate,
      status = PrisonerStatus.SUSPENDED,
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))
    whenever(attendanceSuspensionDomainService.suspendFutureAttendancesForAllocation(any(), eq(allocation), eq(false))).thenReturn(
      listOf(attendance(1), attendance(2)),
    )

    service.suspend(prisonCode, suspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    allocationCaptor.firstValue.first().status(PrisonerStatus.SUSPENDED) isBool true

    verify(attendanceSuspensionDomainService).suspendFutureAttendancesForAllocation(any(), eq(allocation), eq(false))
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 2L)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `suspending a PENDING allocation immediately sets the suspension start date equal to the allocation start date`() {
    val allocation = allocation(startDate = LocalDate.now().plusDays(1))
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = LocalDate.now(),
      status = PrisonerStatus.SUSPENDED,
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    service.suspend(prisonCode, suspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    with(allocationCaptor.firstValue.first()) {
      status(PrisonerStatus.PENDING) isBool true
      plannedSuspension()!!.startDate() isEqualTo LocalDate.now().plusDays(1)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `suspension start date in the future does not immediately suspend the allocation`() {
    val allocation = allocation()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = allocation.startDate.plusWeeks(1),
      status = PrisonerStatus.SUSPENDED,
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    service.suspend(prisonCode, suspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    allocationCaptor.firstValue.first().status(PrisonerStatus.ACTIVE) isBool true

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoInteractions(attendanceSuspensionDomainService)
  }

  @Test
  fun `allocation cannot be suspended if it is already suspended`() {
    val allocation = allocation(withPlannedSuspensions = true).apply { activatePlannedSuspension() }
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = allocation.startDate.plusWeeks(1),
      status = PrisonerStatus.SUSPENDED,
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    assertThatThrownBy {
      service.suspend(prisonCode, suspendPrisonerRequest, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation 0: Is already suspended.")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `suspension end date throws error if no suspension to end`() {
    val allocation = allocation()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val unSuspendPrisonerRequest = UnsuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendUntil = allocation.startDate.plusWeeks(1),
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    assertThatThrownBy {
      service.unsuspend(prisonCode, unSuspendPrisonerRequest, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation 0: Must be suspended to unsuspend it.")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `suspension gets ended if the allocation start date is later than the given suspendUntil late`() {
    val allocation = allocation(withPlannedSuspensions = true)
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val unSuspendPrisonerRequest = UnsuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendUntil = allocation.startDate.minusDays(1),
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    service.unsuspend(prisonCode, unSuspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    allocationCaptor.firstValue.first().plannedSuspension() isEqualTo null

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `suspension end date throws error if end date is after the allocation end date`() {
    val allocation = allocation(withPlannedSuspensions = true).apply { endDate = TimeSource.tomorrow() }
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val unSuspendPrisonerRequest = UnsuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendUntil = allocation.plannedEndDate()!!.plusDays(1),
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    assertThatThrownBy {
      service.unsuspend(prisonCode, unSuspendPrisonerRequest, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation 0: Suspension end date must be on or before the allocation end date: ${allocation.plannedEndDate()!!.toIsoDate()}")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `suspension end date gets set`() {
    val allocation = allocation(withPlannedSuspensions = true)
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val unSuspendPrisonerRequest = UnsuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendUntil = allocation.startDate.plusDays(1),
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    service.unsuspend(prisonCode, unSuspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    with(allocationCaptor.firstValue.first().plannedSuspension()!!) {
      endDate() isEqualTo allocation.startDate.plusDays(1)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `suspension end date today immediately resets already suspended allocations`() {
    val allocation = allocation(withPlannedSuspensions = true).apply { activatePlannedSuspension() }
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    allocation.status(PrisonerStatus.SUSPENDED) isBool true

    val unSuspendPrisonerRequest = UnsuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendUntil = LocalDate.now(),
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))
    whenever(attendanceSuspensionDomainService.resetSuspendedFutureAttendancesForAllocation(any(), eq(allocation))).thenReturn(
      listOf(attendance(1), attendance(2)),
    )

    service.unsuspend(prisonCode, unSuspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    allocationCaptor.firstValue.first().status(PrisonerStatus.ACTIVE) isBool true

    verify(attendanceSuspensionDomainService).resetSuspendedFutureAttendancesForAllocation(any(), eq(allocation))
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 2L)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `suspension with prisoner status SUSPENDED gets created`() {
    val allocation = allocation()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = allocation.startDate.plusWeeks(1),
      status = PrisonerStatus.SUSPENDED,
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    service.suspend(prisonCode, suspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    assertThat(allocationCaptor.firstValue.first().plannedSuspension()).isNotNull
    with(allocationCaptor.firstValue.first()) {
      prisonerStatus == PrisonerStatus.SUSPENDED
      plannedSuspension()!!.paid() isEqualTo false
    }
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoMoreInteractions(outboundEventsService)
    verifyNoInteractions(caseNotesApiClient)
  }

  @Test
  fun `suspension with prisoner status SUSPENDED_WITH_PAY gets set`() {
    val allocation = allocation()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val suspendPrisonerRequest = SuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendFrom = allocation.startDate.plusWeeks(1),
      status = PrisonerStatus.SUSPENDED_WITH_PAY,
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    service.suspend(prisonCode, suspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    assertThat(allocationCaptor.firstValue.first().plannedSuspension()).isNotNull
    with(allocationCaptor.firstValue.first()) {
      prisonerStatus == PrisonerStatus.SUSPENDED_WITH_PAY
      plannedSuspension()!!.paid() isEqualTo true
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoMoreInteractions(outboundEventsService)
    verifyNoInteractions(caseNotesApiClient)
  }

  @Test
  fun `unsuspension of a paid suspenson is successful`() {
    val allocation = allocation(withPlannedSuspensions = true, withPaidSuspension = true)
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val unSuspendPrisonerRequest = UnsuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendUntil = allocation.startDate.plusDays(1),
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    service.unsuspend(prisonCode, unSuspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    with(allocationCaptor.firstValue.first().plannedSuspension()!!) {
      endDate() isEqualTo allocation.startDate.plusDays(1)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `unsuspension of paid suspenson end date gets set`() {
    val allocation = allocation(withPlannedSuspensions = true, withPaidSuspension = true)
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val unSuspendPrisonerRequest = UnsuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendUntil = allocation.startDate.plusDays(1),
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))

    service.unsuspend(prisonCode, unSuspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    with(allocationCaptor.firstValue.first().plannedSuspension()!!) {
      endDate() isEqualTo allocation.startDate.plusDays(1)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `unsuspension end date today immediately resets already paid suspension allocations`() {
    val allocation = allocation(withPlannedSuspensions = true, withPaidSuspension = true).apply { activatePlannedSuspension(PrisonerStatus.SUSPENDED_WITH_PAY) }
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    allocation.status(PrisonerStatus.SUSPENDED_WITH_PAY) isBool true

    val unSuspendPrisonerRequest = UnsuspendPrisonerRequest(
      prisonerNumber = "A1234AA",
      allocationIds = listOf(allocation.allocationId),
      suspendUntil = LocalDate.now(),
    )

    whenever(allocationRepository.findAllById(setOf(allocationId))).thenReturn(listOf(allocation))
    whenever(attendanceSuspensionDomainService.resetSuspendedFutureAttendancesForAllocation(any(), eq(allocation))).thenReturn(
      listOf(attendance(1), attendance(2)),
    )

    service.unsuspend(prisonCode, unSuspendPrisonerRequest, "user")

    verify(allocationRepository).saveAllAndFlush(allocationCaptor.capture())

    allocationCaptor.firstValue.first().status(PrisonerStatus.ACTIVE) isBool true

    verify(attendanceSuspensionDomainService).resetSuspendedFutureAttendancesForAllocation(any(), eq(allocation))
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 2L)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `suspension with any prisoner status that is not SUSPENDED or SUSPENDED_WITH_PAY fails validation`() {
    val allocation = allocation()
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    PrisonerStatus.allExcuding(PrisonerStatus.SUSPENDED, PrisonerStatus.SUSPENDED_WITH_PAY).forEach { status ->
      val suspendPrisonerRequest = SuspendPrisonerRequest(
        prisonerNumber = "A1234AA",
        allocationIds = listOf(allocation.allocationId),
        suspendFrom = LocalDate.now(),
        status = status,
      )

      assertThatThrownBy {
        service.suspend(prisonCode, suspendPrisonerRequest, "user")
      }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Only 'SUSPENDED' or 'SUSPENDED_WITH_PAY' are allowed for status")
    }
    verifyNoInteractions(outboundEventsService)
  }
}

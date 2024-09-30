package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ExclusionsFilter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.deallocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.mediumPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonerAllocations
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

class AllocationsServiceTest {
  private val allocationRepository: AllocationRepository = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val scheduleRepository: ActivityScheduleRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val manageAttendancesService: ManageAttendancesService = mock()
  private val service: AllocationsService = AllocationsService(allocationRepository, prisonPayBandRepository, scheduleRepository, TransactionHandler(), outboundEventsService, manageAttendancesService)
  private val activeAllocation = activityEntity().schedules().first().allocations().first()
  private val allocationCaptor = argumentCaptor<Allocation>()
  private val activityScheduleCaptor = argumentCaptor<ActivitySchedule>()

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `find allocations for collection of prisoners`() {
    whenever(allocationRepository.findByPrisonCodeAndPrisonerNumbers("MDI", listOf("ABC123")))
      .thenReturn(listOf(activeAllocation))

    assertThat(
      service.findByPrisonCodeAndPrisonerNumbers(
        "MDI",
        setOf("ABC123"),
      ),
    ).isEqualTo(listOf(activeAllocation).toModelPrisonerAllocations())
  }

  @Test
  fun `find allocations does not return ended allocations`() {
    val endedAllocation = activeAllocation.copy(prisonerStatus = PrisonerStatus.ENDED)

    whenever(allocationRepository.findByPrisonCodeAndPrisonerNumbers("MDI", listOf("ABC123", "CAB321")))
      .thenReturn(listOf(activeAllocation, endedAllocation))

    assertThat(
      service.findByPrisonCodeAndPrisonerNumbers(
        "MDI",
        setOf("ABC123", "CAB321"),
      ),
    ).isEqualTo(listOf(activeAllocation).toModelPrisonerAllocations())
  }

  @Test
  fun `transformed allocation returned when find by id`() {
    val expected = allocation()

    addCaseloadIdToRequestHeader("MDI")
    whenever(scheduleRepository.findById(expected.activitySchedule.activityScheduleId)).thenReturn(Optional.of(expected.activitySchedule))
    whenever(allocationRepository.findById(expected.allocationId)).thenReturn(Optional.of(expected))

    assertThat(service.getAllocationById(expected.allocationId)).isEqualTo(expected.toModel())
  }

  @Test
  fun `find by id throws entity not found for unknown allocation`() {
    whenever(allocationRepository.findById(any())).thenReturn(Optional.empty())

    assertThatThrownBy {
      service.getAllocationById(1)
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Allocation 1 not found")
  }

  @Test
  fun `updateAllocation - update start date - no new attendances needed`() {
    val allocation = allocation(startDate = TimeSource.tomorrow())
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)

    allocation.startDate = LocalDate.now().plusDays(1)

    val updateAllocationRequest = AllocationUpdateRequest(startDate = TimeSource.tomorrow().plusDays(1))

    service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")

    verify(allocationRepository).saveAndFlush(allocationCaptor.capture())

    val savedAllocation = allocationCaptor.firstValue

    assertThat(savedAllocation.startDate).isEqualTo(TimeSource.tomorrow().plusDays(1))
    assertThat(savedAllocation.prisonerStatus).isEqualTo(PrisonerStatus.PENDING)

    inOrder(manageAttendancesService) {
      verify(manageAttendancesService).createAnyAttendancesForToday(eq(null), any())
      verify(manageAttendancesService).saveAttendances(eq(emptyList()), any())
      verifyNoMoreInteractions()
    }
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
  }

  @Test
  fun `updateAllocation - update start date to today - new attendances needed and status becomes active`() {
    val allocation = allocation(startDate = TimeSource.today().plusDays(3), withExclusions = true)
    allocation.activitySchedule.activity.startDate = TimeSource.today().minusDays(1)
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    assertThat(allocation.exclusions(ExclusionsFilter.ACTIVE).first().startDate).isEqualTo(TimeSource.today().plusDays(3))

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)

    val attendance1: Attendance = mock()
    val attendance2: Attendance = mock()
    val newAttendances: List<Attendance> = listOf(attendance1, attendance2)
    whenever(manageAttendancesService.createAnyAttendancesForToday(ArgumentMatchers.eq(123L), any())) doReturn newAttendances
    whenever(manageAttendancesService.saveAttendances(eq(newAttendances), any())) doReturn newAttendances

    allocation.startDate = LocalDate.now().plusDays(1)

    val updateAllocationRequest = AllocationUpdateRequest(startDate = TimeSource.today(), scheduleInstanceId = 123L)

    service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")

    verify(allocationRepository).saveAndFlush(allocationCaptor.capture())

    val savedAllocation = allocationCaptor.firstValue

    assertThat(savedAllocation.startDate).isEqualTo(TimeSource.today())
    assertThat(savedAllocation.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE)
    // If there are future exclusions then their start date should be match ew allocation start date
    assertThat(allocation.exclusions(ExclusionsFilter.ACTIVE).first().startDate).isEqualTo(TimeSource.today())

    inOrder(manageAttendancesService) {
      verify(manageAttendancesService).createAnyAttendancesForToday(eq(123L), allocationCaptor.capture())
      assertThat(allocationCaptor.firstValue.startDate).isEqualTo(TimeSource.today())
      verify(manageAttendancesService).saveAttendances(eq(newAttendances), activityScheduleCaptor.capture())
      assertThat(activityScheduleCaptor.firstValue).isEqualTo(allocation.activitySchedule)
      verify(manageAttendancesService).sendCreatedEvent(eq(attendance1))
      verify(manageAttendancesService).sendCreatedEvent(eq(attendance2))
    }
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
  }

  @Test
  fun `updateAllocation - update end date`() {
    val allocation = allocation().apply { endDate = null }
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)

    val updateAllocationRequest = AllocationUpdateRequest(endDate = TimeSource.tomorrow(), reasonCode = "OTHER")

    service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")

    verify(allocationRepository).saveAndFlush(allocationCaptor.capture())

    assertThat(allocationCaptor.firstValue.endDate).isEqualTo(TimeSource.tomorrow())
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
  }

  @Test
  fun `updateAllocation - update end date when it had been set before`() {
    val allocation = deallocation(endDate = TimeSource.tomorrow().plusDays(2))
    allocation.deallocateOn(TimeSource.tomorrow().plusDays(2), DeallocationReason.HEALTH, "Test User")
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)

    val updateAllocationRequest = AllocationUpdateRequest(endDate = TimeSource.tomorrow(), reasonCode = "OTHER")

    service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")

    verify(allocationRepository).saveAndFlush(allocationCaptor.capture())

    assertThat(allocationCaptor.firstValue.endDate).isEqualTo(TimeSource.tomorrow())
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
  }

  @Test
  fun `updateAllocation - update pay band`() {
    val allocation = allocation().also { assertThat(it.payBand).isEqualTo(lowPayBand) }
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val updateAllocationRequest = AllocationUpdateRequest(payBandId = mediumPayBand.prisonPayBandId)

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)
    whenever(prisonPayBandRepository.findById(mediumPayBand.prisonPayBandId)).thenReturn(Optional.of(mediumPayBand))

    service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")

    verify(allocationRepository).saveAndFlush(allocationCaptor.capture())

    assertThat(allocationCaptor.firstValue.payBand).isEqualTo(mediumPayBand)
    assertThat(allocation.payBand).isEqualTo(mediumPayBand)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
  }

  @Test
  fun `updateAllocation - invalid pay band`() {
    val allocation = allocation()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode
    val updateAllocationRequest = AllocationUpdateRequest(payBandId = 99)

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(prisonPayBandRepository.findById(updateAllocationRequest.payBandId!!)).thenReturn(Optional.empty())

    assertThatThrownBy {
      service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prison Pay Band 99 not found")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `updateAllocation - update start date - allocation started`() {
    val allocation = allocation(startDate = TimeSource.today())
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)

    val updateAllocationRequest = AllocationUpdateRequest(startDate = TimeSource.tomorrow())

    assertThatThrownBy {
      service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Start date cannot be updated once allocation has started")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `updateAllocation - update start date - allocation start date before activity start date`() {
    val allocation = allocation(startDate = TimeSource.tomorrow().plusDays(1))
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)

    val updateAllocationRequest = AllocationUpdateRequest(startDate = TimeSource.tomorrow())

    assertThatThrownBy {
      service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation start date cannot be before the activity start date or after the activity end date.")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `updateAllocation - update start date - allocation start date cannot be after the activity end date`() {
    val activity = activityEntity(startDate = TimeSource.tomorrow(), endDate = TimeSource.tomorrow().plusDays(1))
    val allocation = activity.schedules().first().allocations().first()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)

    val updateAllocationRequest = AllocationUpdateRequest(startDate = activity.endDate!!.plusDays(1))

    assertThatThrownBy {
      service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation start date cannot be before the activity start date or after the activity end date.")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `updateAllocation - update end date - allocation end date after activity end date`() {
    val activity = activityEntity(startDate = TimeSource.today(), endDate = TimeSource.tomorrow())
    val allocation = activity.schedules().first().allocations().first()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)

    val updateAllocationRequest = AllocationUpdateRequest(endDate = activity.endDate!!.plusDays(1), reasonCode = "OTHER")

    assertThatThrownBy {
      service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation end date cannot be after activity end date")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `updateAllocation - update end date - allocation ended`() {
    val allocation = allocation(startDate = TimeSource.yesterday()).apply { prisonerStatus = PrisonerStatus.ENDED }
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)

    val updateAllocationRequest = AllocationUpdateRequest(endDate = null, reasonCode = "OTHER")

    assertThatThrownBy {
      service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Ended allocations cannot be updated")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `updateAllocation - fails if updating end date without reason`() {
    val allocation = allocation(startDate = TimeSource.yesterday())
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)

    val updateAllocationRequest = AllocationUpdateRequest(endDate = TimeSource.tomorrow())

    assertThatThrownBy {
      service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Reason code must be supplied when setting the allocation end date")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `updateAllocation - fails if allocation not found`() {
    whenever(allocationRepository.findByAllocationIdAndPrisonCode(1, MOORLAND_PRISON_CODE)).thenReturn(null)

    assertThatThrownBy {
      service.updateAllocation(1, AllocationUpdateRequest(startDate = null), MOORLAND_PRISON_CODE, "user")
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Allocation 1 not found at MDI.")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `updateAllocation - fails if allocation start date cannot be in the past`() {
    val allocation = allocation(startDate = TimeSource.tomorrow())
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)

    assertThatThrownBy {
      service.updateAllocation(allocationId, AllocationUpdateRequest(startDate = TimeSource.yesterday()), prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation start date must not be in the past")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `updateAllocation - fails if allocation starts today but schedule instance id is null`() {
    val allocation = allocation(startDate = TimeSource.tomorrow())
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)

    assertThatThrownBy {
      service.updateAllocation(allocationId, AllocationUpdateRequest(startDate = TimeSource.today()), prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The next session must be provided when allocation start date is today")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `updateAllocation - fails if allocation start date after end date`() {
    val allocation = allocation(startDate = TimeSource.tomorrow()).apply { endDate = TimeSource.tomorrow() }
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)

    assertThatThrownBy {
      service.updateAllocation(allocationId, AllocationUpdateRequest(startDate = allocation.endDate?.plusDays(1)), prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation start date cannot be after allocation end date")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `updateAllocation - update reasonCode`() {
    val allocation = allocation()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)

    val updateAllocationRequest = AllocationUpdateRequest(endDate = TimeSource.tomorrow(), reasonCode = "HEALTH")

    service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")

    verify(allocationRepository).saveAndFlush(allocationCaptor.capture())

    assertThat(allocationCaptor.firstValue.plannedDeallocation?.plannedReason?.name).isEqualTo("HEALTH")
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
  }

  @Test
  fun `updateAllocation - invalid reasonCode`() {
    val allocation = allocation()
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)

    val updateAllocationRequest = AllocationUpdateRequest(endDate = TimeSource.tomorrow(), reasonCode = "ALPHA")

    assertThatThrownBy {
      service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Invalid deallocation reason specified 'ALPHA'")

    verifyNoInteractions(outboundEventsService)
  }

  @Test
  fun `updateAllocation - update exclusions`() {
    val allocation = allocation().also { it.exclusions(ExclusionsFilter.ACTIVE) hasSize 0 }
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val updateAllocationRequest = AllocationUpdateRequest(
      exclusions = listOf(
        Slot(
          weekNumber = 1,
          timeSlot = TimeSlot.AM,
          monday = true,
        ),
      ),
    )

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)

    service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")

    verify(allocationRepository).saveAndFlush(allocationCaptor.capture())

    allocationCaptor.firstValue.exclusions(ExclusionsFilter.ACTIVE) hasSize 1
    allocationCaptor.firstValue.exclusions(ExclusionsFilter.ACTIVE).first().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY)
    allocationCaptor.firstValue.exclusions(ExclusionsFilter.ACTIVE).first().startDate isEqualTo LocalDate.now().plusDays(1)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
  }

  @Test
  fun `updateAllocation - update exclusions with multiple schedule slots for a timeslots`() {
    val allocation = allocation().also { it.exclusions(ExclusionsFilter.ACTIVE) hasSize 0 }
    val allocationId = allocation.allocationId
    allocation.activitySchedule.addSlot(
      1,
      LocalTime.of(9, 40) to LocalTime.of(11, 35),
      setOf(DayOfWeek.TUESDAY),
      TimeSlot.AM,
    )
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val updateAllocationRequest = AllocationUpdateRequest(
      exclusions = listOf(
        Slot(
          weekNumber = 1,
          timeSlot = TimeSlot.AM,
          monday = true,
          tuesday = true,
        ),
      ),
    )

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)

    service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")

    verify(allocationRepository).saveAndFlush(allocationCaptor.capture())

    allocationCaptor.firstValue.exclusions(ExclusionsFilter.ACTIVE) hasSize 1
    allocationCaptor.firstValue.exclusions(ExclusionsFilter.ACTIVE).first().getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
    allocationCaptor.firstValue.exclusions(ExclusionsFilter.ACTIVE).first().startDate isEqualTo LocalDate.now().plusDays(1)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
  }

  @Test
  fun `updateAllocation - update exclusions removes existing exclusions which are not present in the update request`() {
    val activity = activityEntity(noSchedules = true)
    val schedule = activitySchedule(activity, noSlots = true, scheduleWeeks = 2)

    val slot = schedule.addSlot(
      weekNumber = 1,
      slotTimes = LocalTime.NOON to LocalTime.NOON.plusHours(1),
      daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
      timeSlot = TimeSlot.PM,
    )

    schedule.addSlot(
      weekNumber = 2,
      slotTimes = LocalTime.NOON to LocalTime.NOON.plusHours(1),
      daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
      timeSlot = TimeSlot.PM,
    )

    val allocation = schedule.allocatePrisoner(
      prisonerNumber = "A1111BB".toPrisonerNumber(),
      bookingId = 20002,
      payBand = lowPayBand,
      allocatedBy = "Mr Blogs",
      startDate = activity.startDate,
    )
      .apply {
        updateExclusion(
          exclusionSlot = Slot(
            weekNumber = slot.weekNumber,
            timeSlot = slot.timeSlot,
            friday = true,
          ),
          startDate = LocalDate.now().plusDays(1),
        )
      }
      .also {
        it.exclusions(ExclusionsFilter.ACTIVE) hasSize 1
        with(it.exclusions(ExclusionsFilter.ACTIVE).first()) {
          weekNumber isEqualTo 1
          getDaysOfWeek() isEqualTo setOf(DayOfWeek.FRIDAY)
        }
      }

    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)

    val updateAllocationRequest = AllocationUpdateRequest(
      exclusions = listOf(
        Slot(
          weekNumber = 2,
          timeSlot = TimeSlot.PM,
          thursday = true,
        ),
      ),
    )

    service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")

    verify(allocationRepository).saveAndFlush(allocationCaptor.capture())

    allocationCaptor.firstValue.exclusions(ExclusionsFilter.ACTIVE) hasSize 1
    with(allocationCaptor.firstValue.exclusions(ExclusionsFilter.ACTIVE).first()) {
      weekNumber isEqualTo 2
      getDaysOfWeek() isEqualTo setOf(DayOfWeek.THURSDAY)
    }
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, allocationId)
  }

  @Test
  fun `updateAllocation - update exclusions fails if week number and time slot combination returns no slots`() {
    val allocation = allocation().also { it.exclusions(ExclusionsFilter.ACTIVE) hasSize 0 }
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    val updateAllocationRequest = AllocationUpdateRequest(
      exclusions = listOf(
        Slot(
          weekNumber = 3,
          timeSlot = TimeSlot.AM,
          monday = true,
        ),
      ),
    )

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)

    assertThatThrownBy {
      service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Updating allocation with id 0: No AM slots in week number 3")

    verifyNoInteractions(outboundEventsService)
  }
}

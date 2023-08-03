package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.deallocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.mediumPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonerAllocations
import java.time.LocalDate
import java.util.Optional

class AllocationServiceTest {
  private val allocationRepository: AllocationRepository = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val scheduleRepository: ActivityScheduleRepository = mock()
  private val service: AllocationsService = AllocationsService(allocationRepository, prisonPayBandRepository, scheduleRepository)
  private val activeAllocation = activityEntity().schedules().first().allocations().first()
  private val allocationCaptor = argumentCaptor<Allocation>()

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
  fun `updateAllocation - update start date`() {
    val allocation = allocation(startDate = TimeSource.tomorrow())
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)

    allocation.startDate = LocalDate.now().plusDays(1)

    val updateAllocationRequest = AllocationUpdateRequest(startDate = TimeSource.tomorrow().plusDays(1))

    service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")

    verify(allocationRepository).saveAndFlush(allocationCaptor.capture())

    assertThat(allocationCaptor.firstValue.startDate).isEqualTo(TimeSource.tomorrow().plusDays(1))
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
  }

  @Test
  fun `updateAllocation - update end date when it had been set before`() {
    var allocation = deallocation(endDate = TimeSource.tomorrow().plusDays(2))
    allocation.deallocateOn(TimeSource.tomorrow().plusDays(2), DeallocationReason.HEALTH, "Test User")
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)
    whenever(allocationRepository.saveAndFlush(any())).thenReturn(allocation)

    val updateAllocationRequest = AllocationUpdateRequest(endDate = TimeSource.tomorrow(), reasonCode = "OTHER")

    service.updateAllocation(allocationId, updateAllocationRequest, prisonCode, "user")

    verify(allocationRepository).saveAndFlush(allocationCaptor.capture())

    assertThat(allocationCaptor.firstValue.endDate).isEqualTo(TimeSource.tomorrow())
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
  }

  @Test
  fun `updateAllocation - fails if allocation not found`() {
    whenever(allocationRepository.findByAllocationIdAndPrisonCode(1, moorlandPrisonCode)).thenReturn(null)

    assertThatThrownBy {
      service.updateAllocation(1, AllocationUpdateRequest(startDate = null), moorlandPrisonCode, "user")
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Allocation 1 not found.")
  }

  @Test
  fun `updateAllocation - fails if allocation start date not in future`() {
    val allocation = allocation(startDate = TimeSource.tomorrow())
    val allocationId = allocation.allocationId
    val prisonCode = allocation.activitySchedule.activity.prisonCode

    whenever(allocationRepository.findByAllocationIdAndPrisonCode(allocationId, prisonCode)).thenReturn(allocation)

    assertThatThrownBy {
      service.updateAllocation(allocationId, AllocationUpdateRequest(startDate = TimeSource.today()), prisonCode, "user")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation start date must be in the future")
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
  }
}

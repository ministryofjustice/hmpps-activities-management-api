package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.read
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonerAllocations
import java.time.LocalDate
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation as AllocationEntity

class AllocationServiceTest {
  private val allocationRepository: AllocationRepository = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val service: AllocationsService = AllocationsService(allocationRepository, prisonPayBandRepository)
  private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
  private val activeAllocation = activityEntity().schedules().first().allocations().first()

  @Captor
  private lateinit var allocationEntityCaptor: ArgumentCaptor<AllocationEntity>

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
    val updateAllocationRequest: AllocationUpdateRequest = mapper.read("allocation/allocation-update-request-1.json")

    val allocationEntity: AllocationEntity = mapper.read("allocation/allocation-entity-1.json")

    whenever(allocationRepository.findById(1)).thenReturn(Optional.of(allocationEntity))

    whenever(allocationRepository.saveAndFlush(allocationEntityCaptor.capture())).thenReturn(allocationEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))

    allocationEntity.startDate = LocalDate.now().plusDays(1)

    service.updateAllocation(1, updateAllocationRequest, moorlandPrisonCode, "user")

    val allocationArg: AllocationEntity = allocationEntityCaptor.value

    verify(allocationRepository).saveAndFlush(allocationArg)

    with(allocationArg) {
      assertThat(startDate).isEqualTo("2023-01-01")
    }
  }

  @Test
  fun `updateAllocation - update end date`() {
    val updateAllocationRequest: AllocationUpdateRequest = mapper.read("allocation/allocation-update-request-2.json")

    val allocationEntity: AllocationEntity = mapper.read("allocation/allocation-entity-1.json")

    whenever(allocationRepository.findById(1)).thenReturn(Optional.of(allocationEntity))

    whenever(allocationRepository.saveAndFlush(allocationEntityCaptor.capture())).thenReturn(allocationEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))

    service.updateAllocation(1, updateAllocationRequest, moorlandPrisonCode, "user")

    val allocationArg: AllocationEntity = allocationEntityCaptor.value

    verify(allocationRepository).saveAndFlush(allocationArg)

    with(allocationArg) {
      assertThat(endDate).isEqualTo("2023-12-31")
    }
  }

  @Test
  fun `updateAllocation - update pay band`() {
    val updateAllocationRequest: AllocationUpdateRequest = mapper.read("allocation/allocation-update-request-3.json")

    val allocationEntity: AllocationEntity = mapper.read("allocation/allocation-entity-1.json")

    whenever(allocationRepository.findById(1)).thenReturn(Optional.of(allocationEntity))

    whenever(allocationRepository.saveAndFlush(allocationEntityCaptor.capture())).thenReturn(allocationEntity)
    whenever(prisonPayBandRepository.findById(12)).thenReturn(Optional.of(prisonPayBandsLowMediumHigh(moorlandPrisonCode, 10)[1]))

    service.updateAllocation(1, updateAllocationRequest, moorlandPrisonCode, "user")

    val allocationArg: AllocationEntity = allocationEntityCaptor.value

    verify(allocationRepository).saveAndFlush(allocationArg)

    with(allocationArg) {
      assertThat(payBand.prisonPayBandId).isEqualTo(12)
    }
  }

  @Test
  fun `updateAllocation - invalid pay band`() {
    val updateAllocationRequest: AllocationUpdateRequest = mapper.read("allocation/allocation-update-request-4.json")

    val allocationEntity: AllocationEntity = mapper.read("allocation/allocation-entity-1.json")

    whenever(allocationRepository.findById(1)).thenReturn(Optional.of(allocationEntity))

    whenever(allocationRepository.saveAndFlush(allocationEntityCaptor.capture())).thenReturn(allocationEntity)
    whenever(prisonPayBandRepository.findById(14)).thenReturn(Optional.empty())

    assertThatThrownBy { service.updateAllocation(1, updateAllocationRequest, moorlandPrisonCode, "user") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prison Pay Band 14 not found")
  }

  @Test
  fun `updateAllocation - update start date - allocation started`() {
    val updateAllocationRequest: AllocationUpdateRequest = mapper.read("allocation/allocation-update-request-1.json")

    val allocationEntity: AllocationEntity = mapper.read("allocation/allocation-entity-1.json")

    whenever(allocationRepository.findById(1)).thenReturn(Optional.of(allocationEntity))

    whenever(allocationRepository.saveAndFlush(allocationEntityCaptor.capture())).thenReturn(allocationEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))

    assertThatThrownBy { service.updateAllocation(1, updateAllocationRequest, moorlandPrisonCode, "user") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Start date cannot be updated once allocation has started")
  }

  @Test
  fun `updateAllocation - update start date - allocation start date before activity start date`() {
    val updateAllocationRequest = AllocationUpdateRequest(startDate = TimeSource.yesterday())

    val allocationEntity: AllocationEntity = mapper.read("allocation/allocation-entity-1.json")

    whenever(allocationRepository.findById(1)).thenReturn(Optional.of(allocationEntity))

    whenever(allocationRepository.saveAndFlush(allocationEntityCaptor.capture())).thenReturn(allocationEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))

    allocationEntity.startDate = TimeSource.tomorrow()
    allocationEntity.activitySchedule.activity.startDate = TimeSource.tomorrow()

    assertThatThrownBy { service.updateAllocation(1, updateAllocationRequest, moorlandPrisonCode, "user") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation start date cannot be before the activity start date or after the activity end date.")
  }

  @Test
  fun `updateAllocation - update start date - allocation start date cannot be after the activity end date`() {
    val updateAllocationRequest = AllocationUpdateRequest(startDate = TimeSource.tomorrow())

    val allocationEntity: AllocationEntity = mapper.read("allocation/allocation-entity-1.json")

    whenever(allocationRepository.findById(1)).thenReturn(Optional.of(allocationEntity))

    whenever(allocationRepository.saveAndFlush(allocationEntityCaptor.capture())).thenReturn(allocationEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))

    allocationEntity.startDate = LocalDate.now().plusDays(1)

    allocationEntity.activitySchedule.activity.apply {
      startDate = TimeSource.yesterday()
      endDate = TimeSource.today()
    }

    assertThatThrownBy { service.updateAllocation(1, updateAllocationRequest, moorlandPrisonCode, "user") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation start date cannot be before the activity start date or after the activity end date.")
  }

  @Test
  fun `updateAllocation - update end date - allocation end date after activity end date`() {
    val updateAllocationRequest: AllocationUpdateRequest = mapper.read("allocation/allocation-update-request-2.json")

    val allocationEntity: AllocationEntity = mapper.read("allocation/allocation-entity-1.json")

    whenever(allocationRepository.findById(1)).thenReturn(Optional.of(allocationEntity))

    whenever(allocationRepository.saveAndFlush(allocationEntityCaptor.capture())).thenReturn(allocationEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))

    allocationEntity.endDate = LocalDate.now().plusDays(2)
    allocationEntity.activitySchedule.activity.endDate = LocalDate.now().plusDays(1)

    assertThatThrownBy { service.updateAllocation(1, updateAllocationRequest, moorlandPrisonCode, "user") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation end date cannot be after activity end date")
  }

  @Test
  fun `updateAllocation - update end date - allocation ended`() {
    val updateAllocationRequest: AllocationUpdateRequest = mapper.read("allocation/allocation-update-request-2.json")

    val allocationEntity: AllocationEntity = mapper.read("allocation/allocation-entity-2.json")

    whenever(allocationRepository.findById(1)).thenReturn(Optional.of(allocationEntity))

    whenever(allocationRepository.saveAndFlush(allocationEntityCaptor.capture())).thenReturn(allocationEntity)
    whenever(prisonPayBandRepository.findByPrisonCode(moorlandPrisonCode)).thenReturn(prisonPayBandsLowMediumHigh(offset = 10))

    assertThatThrownBy { service.updateAllocation(1, updateAllocationRequest, moorlandPrisonCode, "user") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Ended allocations cannot be updated")
  }
}

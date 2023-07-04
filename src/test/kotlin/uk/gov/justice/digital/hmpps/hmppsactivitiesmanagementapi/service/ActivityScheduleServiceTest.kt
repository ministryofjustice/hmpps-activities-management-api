package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activeAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.schedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerAllocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonerDeallocationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelAllocations
import java.time.LocalDate
import java.util.Optional

class ActivityScheduleServiceTest {

  private val repository: ActivityScheduleRepository = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val service = ActivityScheduleService(repository, prisonApiClient, prisonPayBandRepository)

  private val prisoner = InmateDetail(
    agencyId = "123",
    offenderNo = "123456",
    inOutStatus = "IN",
    firstName = "Bob",
    lastName = "Bobson",
    activeFlag = true,
    offenderId = 1L,
    rootOffenderId = 1L,
    status = "IN",
    dateOfBirth = LocalDate.of(2001, 10, 1),
    bookingId = 1,
  )

  @Test
  fun `current allocations for a given schedule are returned for current date`() {
    val schedule = schedule().apply {
      allocations().first().startDate = LocalDate.now()
    }

    whenever(repository.findById(1)).thenReturn(Optional.of(schedule))

    val allocations = service.getAllocationsBy(1)

    assertThat(allocations).hasSize(1)
    assertThat(allocations).containsExactlyInAnyOrder(*schedule.allocations().toModelAllocations().toTypedArray())
  }

  @Test
  fun `ended allocations for a given schedule are not returned`() {
    val schedule = schedule().apply {
      allocations().first().apply { deallocateNowWithReason(DeallocationReason.ENDED) }
    }

    whenever(repository.findById(1)).thenReturn(Optional.of(schedule))

    assertThat(service.getAllocationsBy(1)).isEmpty()
  }

  @Test
  fun `all current, future and ended allocations for a given schedule are returned`() {
    val active = activeAllocation.copy(allocationId = 1)
    val suspended = activeAllocation.copy(allocationId = 1).apply { prisonerStatus = PrisonerStatus.SUSPENDED }
    val ended =
      active.copy(allocationId = 2, startDate = active.startDate.minusDays(2))
        .apply { endDate = LocalDate.now().minusDays(1) }
    val future = active.copy(allocationId = 3, startDate = active.startDate.plusDays(1))
    val schedule = mock<ActivitySchedule>()

    whenever(schedule.allocations()).thenReturn(listOf(active, suspended, ended, future))
    whenever(repository.findById(1)).thenReturn(Optional.of(schedule))

    val allocations = service.getAllocationsBy(1, false)
    assertThat(allocations).hasSize(4)
    assertThat(allocations).containsExactlyInAnyOrder(
      *listOf(active, suspended, ended, future).toModelAllocations().toTypedArray(),
    )
  }

  @Test
  fun `throws entity not found exception for unknown activity schedule`() {
    assertThatThrownBy { service.getAllocationsBy(-99) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity Schedule -99 not found")
  }

  @Test
  fun `can deallocate a prisoner from activity schedule`() {
    val schedule = mock<ActivitySchedule>()

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))

    service.deallocatePrisoners(
      schedule.activityScheduleId,
      PrisonerDeallocationRequest(listOf("1"), DeallocationReason.OTHER.name, TimeSource.tomorrow()),
      "by test",
    )

    verify(schedule).deallocatePrisonerOn("1", TimeSource.tomorrow(), DeallocationReason.OTHER, "by test")
    verify(repository).saveAndFlush(schedule)
  }

  @Test
  fun `can deallocate multiple prisoners from activity schedule`() {
    val schedule = mock<ActivitySchedule>()

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))

    service.deallocatePrisoners(
      schedule.activityScheduleId,
      PrisonerDeallocationRequest(listOf("1", "2"), DeallocationReason.OTHER.name, TimeSource.tomorrow()),
      "by test",
    )

    verify(schedule).deallocatePrisonerOn("1", TimeSource.tomorrow(), DeallocationReason.OTHER, "by test")
    verify(schedule).deallocatePrisonerOn("2", TimeSource.tomorrow(), DeallocationReason.OTHER, "by test")
    verify(repository).saveAndFlush(schedule)
  }

  @Test
  fun `throws entity not found exception for unknown activity schedule when try and deallocate`() {
    val schedule = activitySchedule(activityEntity())
    val allocation = schedule.allocations().first()

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.empty())

    assertThatThrownBy {
      service.deallocatePrisoners(
        schedule.activityScheduleId,
        PrisonerDeallocationRequest(
          listOf(allocation.prisonerNumber),
          DeallocationReason.RELEASED.name,
          TimeSource.tomorrow(),
        ),
        "by test",
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity Schedule ${schedule.activityScheduleId} not found")
  }

  @Test
  fun `throws exception for invalid reason codes on attempted deallocation`() {
    val schedule = mock<ActivitySchedule>()

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))

    DeallocationReason.values().filter { !it.displayed }.map { it.name }.forEach { reasonCode ->
      assertThatThrownBy {
        service.deallocatePrisoners(
          schedule.activityScheduleId,
          PrisonerDeallocationRequest(
            listOf("123456"),
            reasonCode,
            TimeSource.tomorrow(),
          ),
          "by test",
        )
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Invalid deallocation reason specified '$reasonCode'")
    }
  }

  @Test
  fun `allocate throws exception for start date before activity start date`() {
    var schedule = activitySchedule(activityEntity())
    schedule.activity.startDate = LocalDate.now().plusDays(2)

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))
    whenever(prisonPayBandRepository.findByPrisonCode("123")).thenReturn(prisonPayBandsLowMediumHigh("123"))
    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = false)).doReturn(Mono.just(prisoner))

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          "123456",
          1,
          TimeSource.tomorrow(),
        ),
        "by test",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation start date cannot be before activity start date")
  }

  @Test
  fun `allocate throws exception for end date after activity end date`() {
    var schedule = activitySchedule(activityEntity())
    schedule.activity.endDate = TimeSource.tomorrow()

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))
    whenever(prisonPayBandRepository.findByPrisonCode("123")).thenReturn(prisonPayBandsLowMediumHigh("123"))
    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = false)).doReturn(Mono.just(prisoner))

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          "123456",
          1,
          TimeSource.tomorrow(),
          TimeSource.tomorrow().plusDays(1),
        ),
        "by test",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation end date cannot be after activity end date")
  }

  @Test
  fun `allocate throws exception for end date before activity start date`() {
    var schedule = activitySchedule(activityEntity())

    whenever(repository.findById(schedule.activityScheduleId)).doReturn(Optional.of(schedule))
    whenever(prisonPayBandRepository.findByPrisonCode("123")).thenReturn(prisonPayBandsLowMediumHigh("123"))
    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = false)).doReturn(Mono.just(prisoner))

    assertThatThrownBy {
      service.allocatePrisoner(
        schedule.activityScheduleId,
        PrisonerAllocationRequest(
          "123456",
          1,
          TimeSource.tomorrow(),
          TimeSource.today(),
        ),
        "by test",
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation end date cannot be before allocation start date")
  }
}

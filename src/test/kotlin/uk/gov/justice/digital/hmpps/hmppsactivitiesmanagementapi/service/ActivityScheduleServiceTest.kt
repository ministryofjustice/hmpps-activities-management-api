package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.schedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelAllocations
import java.time.LocalDate
import java.util.Optional
import javax.persistence.EntityNotFoundException

class ActivityScheduleServiceTest {

  private val repository: ActivityScheduleRepository = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val service = ActivityScheduleService(repository, prisonApiClient)

  @Test
  fun `current allocations for a given schedule are returned for current date`() {
    val schedule = schedule().apply {
      allocations.first().startDate = LocalDate.now()
    }

    whenever(repository.findById(1)).thenReturn(Optional.of(schedule))

    val allocations = service.getAllocationsBy(1)

    assertThat(allocations).hasSize(1)
    assertThat(allocations).containsExactlyInAnyOrder(*schedule.allocations.toModelAllocations().toTypedArray())
  }

  @Test
  fun `future allocations for a given schedule are not returned`() {
    val schedule = schedule().apply {
      allocations.first().startDate = LocalDate.now().plusDays(1)
    }

    whenever(repository.findById(1)).thenReturn(Optional.of(schedule))

    assertThat(service.getAllocationsBy(1)).isEmpty()
  }

  @Test
  fun `ended allocations for a given schedule are not returned`() {
    val schedule = schedule().apply {
      allocations.first().apply {
        startDate = LocalDate.now().minusDays(10)
        endDate = LocalDate.now().minusDays(9)
      }
    }

    whenever(repository.findById(1)).thenReturn(Optional.of(schedule))

    assertThat(service.getAllocationsBy(1)).isEmpty()
  }

  @Test
  fun `all current, future and ended allocations for a given schedule are returned`() {
    val schedule = schedule()
    val active = schedule.allocations.first().copy(allocationId = 1)
    val ended =
      active.copy(allocationId = 2, startDate = LocalDate.now().minusDays(2), endDate = LocalDate.now().minusDays(1))
    val future = active.copy(allocationId = 3, startDate = LocalDate.now().plusDays(1))

    schedule.apply {
      allocations.clear()
      allocations.addAll(listOf(active, ended, future))
    }

    whenever(repository.findById(1)).thenReturn(Optional.of(schedule))

    val allocations = service.getAllocationsBy(1, false)
    assertThat(allocations).hasSize(3)
    assertThat(allocations).containsExactlyInAnyOrder(
      *listOf(active, ended, future).toModelAllocations().toTypedArray()
    )
  }

  @Test
  fun `throws entity not found exception for unknown activity schedule`() {
    assertThatThrownBy { service.getAllocationsBy(-99) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity Schedule -99 not found")
  }
}

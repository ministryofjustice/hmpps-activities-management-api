package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import java.time.LocalDate
import java.util.Optional
import javax.persistence.EntityNotFoundException

class ScheduledInstanceServiceTest {
  private val repository: ScheduledInstanceRepository = mock()
  private val service = ScheduledInstanceService(repository)

  @Test
  fun `getActivityScheduleInstanceById - success`() {
    whenever(repository.findById(1)).thenReturn(Optional.of(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))
    assertThat(service.getActivityScheduleInstanceById(1)).isInstanceOf(ActivityScheduleInstance::class.java)
  }

  @Test
  fun `getActivityScheduleInstanceById - not found`() {
    whenever(repository.findById(1)).thenReturn(Optional.empty())
    assertThatThrownBy { service.getActivityScheduleInstanceById(-1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Scheduled Instance -1 not found")
  }

  @Test
  fun `getActivityScheduleInstancesByDateRange - success`() {
    val prisonCode = "MDI"
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)
    val dateRange = LocalDateRange(startDate, endDate)

    whenever(repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(prisonCode, startDate, endDate))
      .thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

    val result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, null)

    assertThat(result).hasSize(1)
  }

  @Test
  fun `getActivityScheduleInstancesByDateRange - filtered by time slot`() {
    val prisonCode = "MDI"
    val startDate = LocalDate.of(2022, 10, 1)
    val endDate = LocalDate.of(2022, 11, 5)
    val dateRange = LocalDateRange(startDate, endDate)

    whenever(repository.getActivityScheduleInstancesByPrisonCodeAndDateRange(prisonCode, startDate, endDate))
      .thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

    var result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, TimeSlot.PM)
    assertThat(result).hasSize(1)

    result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, TimeSlot.AM)
    assertThat(result).isEmpty()

    result = service.getActivityScheduleInstancesByDateRange(prisonCode, dateRange, TimeSlot.ED)
    assertThat(result).isEmpty()
  }
}

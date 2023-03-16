package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ScheduledInstanceServiceTest {

  @Captor
  private lateinit var updatedFixtureCaptor: ArgumentCaptor<ScheduledInstance>

  private val repository: ScheduledInstanceRepository = mock()
  private val service = ScheduledInstanceService(repository)

  @Nested
  @DisplayName("getActivityScheduleInstanceById")
  inner class GetActivityScheduleInstanceById {

    @Test
    fun `success`() {
      whenever(repository.findById(1)).thenReturn(
        Optional.of(
          ScheduledInstanceFixture.instance(
            id = 1,
            locationId = 22,
          ),
        ),
      )
      assertThat(service.getActivityScheduleInstanceById(1)).isInstanceOf(ActivityScheduleInstance::class.java)
    }

    @Test
    fun `not found`() {
      whenever(repository.findById(1)).thenReturn(Optional.empty())
      assertThatThrownBy { service.getActivityScheduleInstanceById(-1) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Scheduled Instance -1 not found")
    }
  }

  @Nested
  @DisplayName("getActivityScheduleInstancesByDateRange")
  inner class GetActivityScheduleInstancesByDateRange {
    @Test
    fun `success`() {
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
    fun `filtered by time slot`() {
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

  @Nested
  @DisplayName("uncancelScheduledInstance")
  inner class UncancelScheduledInstance {

    @Test
    fun `success`() {
      val instance = mock<ScheduledInstance>()
      whenever(repository.findById(1)).thenReturn(
        Optional.of(instance),
      )

      service.uncancelScheduledInstance(1)

      verify(instance).uncancel()
      verify(repository).save(instance)
    }

    @Test
    fun `scheduled event does not exist`() {
      whenever(repository.findById(1)).thenReturn(Optional.empty())

      val exception = assertThrows<EntityNotFoundException> {
        service.uncancelScheduledInstance(1)
      }

      assertThat(exception.message).isEqualTo("Scheduled Instance 1 not found")
    }
  }
}

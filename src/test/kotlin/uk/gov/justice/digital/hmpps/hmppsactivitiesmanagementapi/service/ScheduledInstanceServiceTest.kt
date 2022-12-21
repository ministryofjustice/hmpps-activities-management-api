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
      .hasMessage("ScheduledInstance -1 not found")
  }

  @Test
  fun `getActivityScheduleInstancesByDateRange - success`() {
    whenever(
      repository.getActivityScheduleInstancesByPrisonerNumberAndDateRange(
        "MDI", "A11111A",
        LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5)
      )
    ).thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

    assertThat(
      service.getActivityScheduleInstancesByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5)),
        null
      )
    ).hasSize(1)
  }

  @Test
  fun `getActivityScheduleInstancesByDateRange - filtered by time slot`() {
    whenever(
      repository.getActivityScheduleInstancesByPrisonerNumberAndDateRange(
        "MDI", "A11111A",
        LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5)
      )
    ).thenReturn(listOf(ScheduledInstanceFixture.instance(id = 1, locationId = 22)))

    assertThat(
      service.getActivityScheduleInstancesByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5)),
        TimeSlot.PM
      )
    ).hasSize(1)

    assertThat(
      service.getActivityScheduleInstancesByDateRange(
        "MDI", "A11111A",
        LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5)),
        TimeSlot.AM
      )
    ).hasSize(0)
  }
}

package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import java.util.Optional
import javax.persistence.EntityNotFoundException

class CapacityServiceTest {
  private val activityCategoryRepository: ActivityCategoryRepository = mock()
  private val activityRepository: ActivityRepository = mock()
  private val activityScheduleRepository: ActivityScheduleRepository = mock()

  private val service = CapacityService(
    activityRepository,
    activityCategoryRepository,
    activityScheduleRepository,
  )

  @Test
  fun `getActivityCategoryCapacityAndAllocated returns an allocation summary for known category ID`() {
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(activityCategory()))
    whenever(activityRepository.getAllByPrisonCodeAndActivityCategory("MDI", activityCategory()))
      .thenReturn(listOf(activityEntity()))

    val returned = service.getActivityCategoryCapacityAndAllocated("MDI", 1)

    assertThat(returned).isInstanceOf(CapacityAndAllocated::class.java)
    assertThat(returned.capacity).isEqualTo(1)
    assertThat(returned.allocated).isEqualTo(1)
  }

  @Test
  fun ` getActivityCategoryCapacityAndAllocated throws entity not found exception for unknown category`() {
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.empty())
    assertThatThrownBy { service.getActivityCategoryCapacityAndAllocated("unknown", 100) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("ActivityCategory 100 not found")
  }

  @Test
  fun `getActivityCapacityAndAllocated returns an allocation summary for known activity ID`() {
    whenever(activityRepository.findById(1)).thenReturn(Optional.of(activityEntity()))

    val returned = service.getActivityCapacityAndAllocated(1)

    assertThat(returned).isInstanceOf(CapacityAndAllocated::class.java)
    assertThat(returned.capacity).isEqualTo(1)
    assertThat(returned.allocated).isEqualTo(1)
  }

  @Test
  fun `getActivityCapacityAndAllocated throws entity not found exception for unknown activity`() {
    whenever(activityRepository.findById(1)).thenReturn(Optional.empty())
    assertThatThrownBy { service.getActivityCapacityAndAllocated(100) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity 100 not found")
  }

  @Test
  fun `getActivityScheduleCapacityAndAllocated returns an allocation summary for known schedule ID`() {
    whenever(activityScheduleRepository.findById(1))
      .thenReturn(Optional.of(activitySchedule(activityEntity())))

    val returned = service.getActivityScheduleCapacityAndAllocated(1)

    assertThat(returned).isInstanceOf(CapacityAndAllocated::class.java)
    assertThat(returned.capacity).isEqualTo(1)
    assertThat(returned.allocated).isEqualTo(1)
  }

  @Test
  fun `getActivityScheduleCapacityAndAllocated throws entity not found exception for unknown schedule`() {
    whenever(activityScheduleRepository.findById(1)).thenReturn(Optional.empty())
    assertThatThrownBy { service.getActivityScheduleCapacityAndAllocated(100) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("ActivitySchedule 100 not found")
  }
}

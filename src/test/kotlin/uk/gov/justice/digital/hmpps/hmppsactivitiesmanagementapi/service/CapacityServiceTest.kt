package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.CapacityAndAllocated
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import java.util.Optional
import javax.persistence.EntityNotFoundException

class CapacityServiceTest {
  private val activityCategoryRepository: ActivityCategoryRepository = mock()
  private val activityRepository: ActivityRepository = mock()
  private val service = CapacityService(activityRepository, activityCategoryRepository)

  @Test
  fun `getActivityCategoryCapacityAndAllocated returns an activity for known category ID`() {
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
      .hasMessage("Activity category 100 not found")
  }

  @Test
  fun `getActivityCapacityAndAllocated returns an activity for known category ID`() {
    whenever(activityRepository.findById(1)).thenReturn(Optional.of(activityEntity()))

    val returned = service.getActivityCapacityAndAllocated(1)

    assertThat(returned).isInstanceOf(CapacityAndAllocated::class.java)
    assertThat(returned.capacity).isEqualTo(1)
    assertThat(returned.allocated).isEqualTo(1)
  }

  @Test
  fun `getActivityCapacityAndAllocated throws entity not found exception for unknown category`() {
    whenever(activityRepository.findById(1)).thenReturn(Optional.empty())
    assertThatThrownBy { service.getActivityCapacityAndAllocated(100) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity 100 not found")
  }
}

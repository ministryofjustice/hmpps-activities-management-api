package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModelLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import java.util.Optional
import javax.persistence.EntityNotFoundException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity

class ActivityServiceTest {
  private val activityRepository: ActivityRepository = mock()
  private val activityCategoryRepository: ActivityCategoryRepository = mock()
  private val activityScheduleRepository: ActivityScheduleRepository = mock()

  private val service = ActivityService(
    activityRepository,
    activityCategoryRepository,
    activityScheduleRepository
  )

  @Test
  fun `getActivityById returns an activity for known activity ID`() {
    whenever(activityRepository.findById(1)).thenReturn(Optional.of(activityEntity()))

    assertThat(service.getActivityById(1)).isInstanceOf(ModelActivity::class.java)
  }

  @Test
  fun `getActivityById throws entity not found exception for unknown activity ID`() {
    assertThatThrownBy { service.getActivityById(-1) }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("-1")
  }

  @Test
  fun `getActivitiesByCategoryInPrison returns list of activities`() {
    val category = activityCategory()

    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.of(category))
    whenever(activityRepository.getAllByPrisonCodeAndActivityCategory("MDI", category))
      .thenReturn(listOf(activityEntity()))

    assertThat(
      service.getActivitiesByCategoryInPrison(
        "MDI",
        1
      )
    ).isEqualTo(listOf(activityEntity()).toModelLite())

    verify(activityRepository, times(1)).getAllByPrisonCodeAndActivityCategory("MDI", category)
  }

  @Test
  fun `getActivitiesByCategoryInPrison throws entity not found exception for unknown category ID`() {
    whenever(activityCategoryRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service.getActivitiesByCategoryInPrison("MDI", 1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity category 1 not found")
  }

  @Test
  fun `getSchedulesForActivity returns list of schedules`() {
    val activity = activityEntity()

    whenever(activityRepository.findById(1)).thenReturn(Optional.of(activity))
    whenever(activityScheduleRepository.getAllByActivity(activity))
      .thenReturn(listOf(activitySchedule(activityEntity())))

    assertThat(service.getSchedulesForActivity(1)).isEqualTo(listOf(activitySchedule(activityEntity())).toModelLite())

    verify(activityScheduleRepository, times(1)).getAllByActivity(activity)
  }

  @Test
  fun `getSchedulesForActivity throws entity not found exception for unknown activity ID`() {
    whenever(activityRepository.findById(1)).thenReturn(Optional.empty())

    assertThatThrownBy { service.getSchedulesForActivity(1) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity 1 not found")
  }
}

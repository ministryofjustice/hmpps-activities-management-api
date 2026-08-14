package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.ActivityCategoryRepository

class ActivitiesFixRotlCategoryServiceTest {
  private val activityRepository: ActivityRepository = mock()
  private val activityCategoryRepository: ActivityCategoryRepository = mock()
  private val activityService: ActivityService = mock()

  private val service = ActivitiesFixRotlCategoryService(activityRepository, activityCategoryRepository, activityService)

  private val rotlCategory = activityCategory("SAA_ROTL").copy(activityCategoryId = 10)

  @Test
  fun `fixes category and location flags for each outside work activity found`() {
    val activity1 = activityEntity(activityId = 1, prisonCode = "MDI", outsideWork = true, noSchedules = true)
    val activity2 = activityEntity(activityId = 2, prisonCode = "PVI", outsideWork = true, onWing = true, noSchedules = true)

    whenever(activityCategoryRepository.findByCode("SAA_ROTL")) doReturn rotlCategory
    whenever(activityRepository.findOutsideWorkActivitiesNeedingRotlFix()) doReturn listOf(activity1, activity2)

    service.fixCategoriesAndLocations()

    verify(activityService).updateActivity(
      eq("MDI"),
      eq(1L),
      eq(
        ActivityUpdateRequest(
          categoryId = 10,
          onWing = false,
          offWing = false,
          inCell = false,
        ),
      ),
      eq("activities-management-admin-1"),
      eq(true),
    )

    verify(activityService).updateActivity(
      eq("PVI"),
      eq(2L),
      eq(
        ActivityUpdateRequest(
          categoryId = 10,
          onWing = false,
          offWing = false,
          inCell = false,
        ),
      ),
      eq("activities-management-admin-1"),
      eq(true),
    )
  }

  @Test
  fun `continues processing remaining activities when one activity update fails`() {
    val activity1 = activityEntity(activityId = 1, prisonCode = "MDI", outsideWork = true, noSchedules = true)
    val activity2 = activityEntity(activityId = 2, prisonCode = "PVI", outsideWork = true, noSchedules = true)

    whenever(activityCategoryRepository.findByCode("SAA_ROTL")) doReturn rotlCategory
    whenever(activityRepository.findOutsideWorkActivitiesNeedingRotlFix()) doReturn listOf(activity1, activity2)
    whenever(activityService.updateActivity(eq("MDI"), any(), any(), any(), any())).thenThrow(RuntimeException("boom"))

    service.fixCategoriesAndLocations()

    verify(activityService, times(1)).updateActivity(eq("MDI"), any(), any(), any(), any())
    verify(activityService, times(1)).updateActivity(eq("PVI"), any(), any(), any(), any())
  }

  @Test
  fun `throws if the ROTL category cannot be found`() {
    whenever(activityCategoryRepository.findByCode("SAA_ROTL")) doReturn null

    assertThatThrownBy { service.fixCategoriesAndLocations() }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Activity category with code SAA_ROTL not found")

    verify(activityRepository, never()).findOutsideWorkActivitiesNeedingRotlFix()
  }

  @Test
  fun `does nothing when there are no activities needing the fix`() {
    whenever(activityCategoryRepository.findByCode("SAA_ROTL")) doReturn rotlCategory
    whenever(activityRepository.findOutsideWorkActivitiesNeedingRotlFix()) doReturn emptyList()

    service.fixCategoriesAndLocations()

    verify(activityService, never()).updateActivity(any(), any(), any(), any(), any())
  }
}

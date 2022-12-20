package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory
import java.time.LocalDate

class ActivityTest {
  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)

  private val activityWithNoEndDate = activityEntity().copy(startDate = today, endDate = null)

  private val activityWithEndDate = activityWithNoEndDate.copy(endDate = tomorrow)

  @Test
  fun `check activity active status that starts today with open end date`() {
    with(activityWithNoEndDate) {
      assertThat(isActive(yesterday)).isFalse
      assertThat(isActive(today)).isTrue
      assertThat(isActive(tomorrow)).isTrue
      assertThat(isActive(tomorrow.plusDays(1000))).isTrue
    }
  }

  @Test
  fun `check activity active status that starts today and ends tomorrow`() {
    with(activityWithEndDate) {
      assertThat(isActive(yesterday)).isFalse
      assertThat(isActive(today)).isTrue
      assertThat(isActive(tomorrow)).isTrue
      assertThat(isActive(tomorrow.plusDays(1))).isFalse
    }
  }

  @Test
  fun `converted to model lite`() {
    val expectedModel = ActivityLite(
      id = 1,
      attendanceRequired = false,
      prisonCode = "123",
      summary = "Maths",
      description = "Maths basic",
      category = ActivityCategory(
        id = 1L,
        code = "category code",
        name = "category name",
        description = "category description"
      )
    )
    assertThat(activityEntity().copy(attendanceRequired = false).toModelLite()).isEqualTo(expectedModel)
  }

  @Test
  fun `List converted to model lite`() {
    val expectedModel = listOf(
      ActivityLite(
        id = 1,
        attendanceRequired = true,
        prisonCode = "123",
        summary = "Maths",
        description = "Maths basic",
        category = ActivityCategory(
          id = 1L,
          code = "category code",
          name = "category name",
          description = "category description"
        )
      )
    )

    assertThat(listOf(activityEntity()).toModelLite()).isEqualTo(expectedModel)
  }
}

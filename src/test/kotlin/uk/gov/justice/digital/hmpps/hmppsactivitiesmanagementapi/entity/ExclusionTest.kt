package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import java.time.DayOfWeek

class ExclusionTest {

  @Test
  fun `setDaysOfWeek - will set day flags`() {
    val schedule = activitySchedule(activityEntity())
    val exclusion = schedule.allocations().last().exclusions.first()

    exclusion.setDaysOfWeek(setOf(DayOfWeek.MONDAY))

    with(exclusion) {
      getDaysOfWeek() isEqualTo activityScheduleSlot.getDaysOfWeek()
      getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY)
    }
  }

  @Test
  fun `setDaysOfWeek - will throw error if selected days are not part of the schedule`() {
    val schedule = activitySchedule(activityEntity())
    val exclusion = schedule.allocations().last().exclusions.first()

    assertThatThrownBy { exclusion.setDaysOfWeek(setOf(DayOfWeek.TUESDAY)) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot set exclusions for slots where the activity does not run")

    with(exclusion) {
      getDaysOfWeek() isEqualTo setOf(DayOfWeek.MONDAY)
    }
  }
}

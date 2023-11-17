package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import java.time.DayOfWeek
import java.time.LocalTime

class ActivityScheduleSlotTest {
  private val activity = activityEntity()
  private val activitySchedule = activity.schedules().first()

  @Test
  fun `conversion to model sets day flags and days-of-week list correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      mondayFlag = true,
      wednesdayFlag = true,
      fridayFlag = true,
      sundayFlag = true,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isTrue
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isTrue
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isTrue
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isTrue
      assertThat(daysOfWeek).containsExactly("Mon", "Wed", "Fri", "Sun")
    }
  }

  @Test
  fun `conversion to model sets monday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      mondayFlag = true,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isTrue
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
      assertThat(daysOfWeek).containsExactly("Mon")
    }
  }

  @Test
  fun `conversion to model sets tuesday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      tuesdayFlag = true,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isTrue
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
      assertThat(daysOfWeek).containsExactly("Tue")
    }
  }

  @Test
  fun `conversion to model sets wednesday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      wednesdayFlag = true,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isTrue
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
      assertThat(daysOfWeek).containsExactly("Wed")
    }
  }

  @Test
  fun `conversion to model sets thursday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      thursdayFlag = true,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isTrue
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
      assertThat(daysOfWeek).containsExactly("Thu")
    }
  }

  @Test
  fun `conversion to model sets fridayFlag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      fridayFlag = true,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isTrue
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
      assertThat(daysOfWeek).containsExactly("Fri")
    }
  }

  @Test
  fun `conversion to model sets saturday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      saturdayFlag = true,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isTrue
      assertThat(sundayFlag).isFalse
      assertThat(daysOfWeek).containsExactly("Sat")
    }
  }

  @Test
  fun `conversion to model sets sunday flag correctly`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      sundayFlag = true,
    )

    with(slot.toModel()) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isTrue
      assertThat(daysOfWeek).containsExactly("Sun")
    }
  }

  @Test
  fun `change day of slot`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      mondayFlag = true,
      sundayFlag = true,
    )

    with(slot) {
      assertThat(mondayFlag).isTrue
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isTrue
    }

    slot.update(setOf(DayOfWeek.TUESDAY))

    with(slot) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isTrue
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
    }
  }

  @Test
  fun `change a slot which has exclusions`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      mondayFlag = true,
      sundayFlag = true,
      exclusions = mutableSetOf(),
    ).apply {
      this.addExclusion(
        Exclusion(
          exclusionId = 1,
          allocation = allocation(null),
          activityScheduleSlot = this,
          mondayFlag = true,
        ),
      )
    }

    with(slot) {
      assertThat(mondayFlag).isTrue
      assertThat(tuesdayFlag).isFalse
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isTrue
    }

    slot.update(setOf(DayOfWeek.TUESDAY))

    with(slot) {
      assertThat(mondayFlag).isFalse
      assertThat(tuesdayFlag).isTrue
      assertThat(wednesdayFlag).isFalse
      assertThat(thursdayFlag).isFalse
      assertThat(fridayFlag).isFalse
      assertThat(saturdayFlag).isFalse
      assertThat(sundayFlag).isFalse
      assertThat(exclusions()).isEmpty()
    }
  }

  @Test
  fun `must provide at least one day on update`() {
    val slot = ActivityScheduleSlot(
      activityScheduleSlotId = 1,
      weekNumber = 1,
      activitySchedule = activitySchedule,
      startTime = LocalTime.now(),
      endTime = LocalTime.now(),
      mondayFlag = true,
      sundayFlag = true,
    )

    assertThatThrownBy { slot.update(emptySet()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("A slot must run on at least one day.")
  }

  @Test
  fun `fails to initialise if week number is not greater than zero`() {
    assertThatThrownBy {
      ActivityScheduleSlot(
        activityScheduleSlotId = 1,
        weekNumber = 0,
        activitySchedule = activitySchedule,
        startTime = LocalTime.now(),
        endTime = LocalTime.now(),
        mondayFlag = true,
        sundayFlag = true,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Week number must be greater than zero.")

    assertThatThrownBy {
      ActivityScheduleSlot(
        activityScheduleSlotId = 1,
        weekNumber = -1,
        activitySchedule = activitySchedule,
        startTime = LocalTime.now(),
        endTime = LocalTime.now(),
        mondayFlag = true,
        sundayFlag = true,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Week number must be greater than zero.")
  }

  @Test
  fun `fails to initialise if week number is not in range`() {
    val activity = activityEntity()
    val schedule = activity.schedules().first().apply { scheduleWeeks = 2 }

    assertThatThrownBy {
      ActivityScheduleSlot(
        activityScheduleSlotId = 1,
        weekNumber = 3,
        activitySchedule = schedule,
        startTime = LocalTime.now(),
        endTime = LocalTime.now(),
        mondayFlag = true,
        sundayFlag = true,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Week number must less than or equal to the number of schedule weeks.")
  }
}
